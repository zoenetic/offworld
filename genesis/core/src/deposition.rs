use crate::{Environment, FieldId, FieldSet, MaterialId, Vec3};

pub struct Deposit {
    pub solidity: f64,
    pub material: MaterialId,
}

#[derive(Default)]
pub struct ColumnState {
    pub deposited: f64,
}

#[derive(Default, Clone, Copy)]
pub struct ColumnContext {
    pub slope: f64,
}

pub trait MaterialSelector {
    fn select(&self, ctx: &ColumnContext, depth: f64) -> MaterialId;
}

pub struct Fixed(pub MaterialId);

impl MaterialSelector for Fixed {
    fn select(&self, _: &ColumnContext, _: f64) -> MaterialId { self.0 }
}

pub struct BySlope {
    pub gentle: MaterialId,
    pub steep: MaterialId,
    pub threshold: f64,
}

impl MaterialSelector for BySlope {
    fn select(&self, ctx: &ColumnContext, _: f64) -> MaterialId {
        if ctx.slope >= self.threshold { self.steep } else { self.gentle }
    }
}

pub struct Draped {
    pub over: MaterialId,
    pub under: MaterialId,
    pub max_depth: f64,
    pub gentle: f64,
    pub steep: f64,
}

impl MaterialSelector for Draped {
    fn select(&self, ctx: &ColumnContext, depth: f64) -> MaterialId {
        let t = ((ctx.slope - self.gentle) / (self.steep - self.gentle)).clamp(0.0, 1.0);
        let scree_depth = self.max_depth * t;
        if depth <= scree_depth { self.over } else { self.under }
    }
}

pub fn deposit_region(
    env: &Environment,
    rule: &dyn DepositionRule,
    origin: Vec3,
    spacing: f64,
    nx: usize,
    ny: usize,
    nz: usize,
) -> FieldSet {
    let mut fields = FieldSet::new(origin, spacing, nx, ny, nz);
    for k in 0..nz {
        for i in 0..nx {
            let mut state = ColumnState::default();
            for j in 0..ny {
                let p = Vec3::new(
                    origin.x + i as f64 * spacing,
                    origin.y + j as f64 * spacing,
                    origin.z + k as f64 * spacing,
                );
                let d = rule.deposit(env, p, spacing, &mut state);
                fields.solidity.set(i, j, k, d.solidity);
                fields.material.set(i, j, k, d.material);
            }
        }
    }
    fields
}

pub trait DepositionRule {
    fn deposit(&self, env: &Environment, p: Vec3, cell_height: f64, state: &mut ColumnState) -> Deposit;
}

pub struct Accrete {
    pub thickness: FieldId,
    pub material: MaterialId,
}

impl DepositionRule for Accrete {
    fn deposit(&self, env: &Environment, p: Vec3, cell_height: f64, state: &mut ColumnState) -> Deposit {
        let target = env.sample(self.thickness, Vec3::new(p.x, 0.0, p.z));
        let remaining = target - state.deposited;
        if remaining <= 0.0 {
            return Deposit { solidity: 0.0, material: MaterialId::NONE };
        }
        let solidity = (remaining / cell_height).min(1.0);
        state.deposited += cell_height;
        Deposit { solidity, material: self.material }
    }
}

pub struct Layer {
    pub material: Box<dyn MaterialSelector>,
    pub thickness: FieldId,
}

impl Layer {
    pub fn fixed(material: MaterialId, thickness: FieldId) -> Self {
        Self { material: Box::new(Fixed(material)), thickness }
    }

    pub fn selected(material: impl MaterialSelector + 'static, thickness: FieldId) -> Self {
        Self { material: Box::new(material), thickness }
    }
}

pub struct LayeredDeposition {
    pub layers: Vec<Layer>,
    pub landform: FieldId,
}

impl LayeredDeposition {
    pub fn surface_height(&self, env: &Environment, x: f64, z: f64) -> f64 {
        self.layers.iter().map(|l| env.sample(l.thickness, Vec3::new(x, 0.0, x))).sum()
    }

    fn context(&self, env: &Environment, x: f64, z: f64) -> ColumnContext {
        let e = 4.0;
        let sample = |a: f64, b: f64| env.sample(self.landform, Vec3::new(a, 0.0, b));
        let hx = (sample(x + e, z) - sample(x - e, z)) / (2.0 * e);
        let hz = (sample(x, z + e) - sample(x, z - e)) / (2.0 * e);
        ColumnContext { slope: (hx * hx + hz * hz).sqrt() }
    }
}

impl DepositionRule for LayeredDeposition {
    fn deposit(&self, env: &Environment, p: Vec3, cell_height: f64, state: &mut ColumnState) -> Deposit {
        let col = Vec3::new(p.x, 0.0, p.z);

        let mut base = 0.0;
        let mut selector: Option<&dyn MaterialSelector> = None;
        for layer in &self.layers {
            let top = base + env.sample(layer.thickness, col);
            if selector.is_none() && state.deposited < top {
                selector = Some(layer.material.as_ref());
            }
            base = top;
        }
        let total = base;
        let remaining = total - state.deposited;
        if remaining <= 0.0 {
            return Deposit { solidity: 0.0, material: MaterialId::NONE };
        }
        let ctx = self.context(env, p.x, p.z);
        let material = selector.map_or(MaterialId::NONE, |s| s.select(&ctx, remaining)); // ← + remaining
        state.deposited += cell_height;
        Deposit { solidity: (remaining / cell_height).min(1.0), material }
    }
}

pub struct Strata {
    pub thickness: FieldId,
    pub bedrock: MaterialId,
    pub stone: MaterialId,
    pub soil: MaterialId,
    pub bedrock_depth: f64,
    pub soil_depth: f64,
}

impl DepositionRule for Strata {
    fn deposit(&self, env: &Environment, p: Vec3, cell_height: f64, state: &mut ColumnState) -> Deposit {
        let target = env.sample(self.thickness, Vec3::new(p.x, 0.0, p.z));
        let remaining = target - state.deposited;
        if remaining <= 0.0 {
            return Deposit { solidity: 0.0, material: MaterialId::NONE };
        }
        let solidity = (remaining / cell_height).min(1.0);
        let from_bottom = state.deposited;
        let from_top = remaining;
        let material = if from_bottom < self.bedrock_depth {
            self.bedrock
        } else if from_top <= self.soil_depth {
            self.soil
        } else {
            self.stone
        };
        state.deposited += cell_height;
        Deposit { solidity, material }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::Constant;

    #[test]
    fn accretes_to_target_height() {
        let mut env = Environment::new();
        let thickness = env.add(Constant(5.0));
        let rule = Accrete { thickness, material: MaterialId(1) };
        let fields = deposit_region(&env, &rule, Vec3::new(0.0, 0.0, 0.0), 1.0, 1, 10, 1);
        for j in 0..10 {
            let want = if j < 5 { 1.0 } else { 0.0 };
            assert_eq!(fields.solidity.get(0, j, 0), want, "cell {j}");
        }
    }

    #[test]
    fn boundary_cell_is_partially_filled() {
        let mut env = Environment::new();
        let thickness = env.add(Constant(4.5));
        let rule = Accrete { thickness, material: MaterialId(1) };
        let fields = deposit_region(&env, &rule, Vec3::new(0.0, 0.0, 0.0), 1.0, 1, 6, 1);
        assert_eq!(fields.solidity.get(0, 3, 0), 1.0);
        assert_eq!(fields.solidity.get(0, 4, 0), 0.5);
        assert_eq!(fields.solidity.get(0, 5, 0), 0.0);
    }

    #[test]
    fn layers_stack_bottom_to_top() {
        let mut env = Environment::new();
        let t = env.add(Constant(3.0));
        let landform = env.add(Constant(1.0));
        let rule = LayeredDeposition {
            layers: vec![
                Layer::fixed(MaterialId(1), t),
                Layer::fixed(MaterialId(2), t),
                Layer::fixed(MaterialId(3), t),
            ],
            landform,
        };
        let f = deposit_region(&env, &rule, Vec3::new(0.0, 0.0, 0.0), 1.0, 1, 12, 1);
        assert_eq!(f.material.get(0, 1, 0), MaterialId(1));
        assert_eq!(f.material.get(0, 4, 0), MaterialId(2));
        assert_eq!(f.material.get(0, 7, 0), MaterialId(3));
        assert_eq!(f.material.get(0, 9, 0), MaterialId::NONE);
        assert_eq!(f.solidity.get(0, 8, 0), 1.0);
        assert_eq!(f.solidity.get(0, 9, 0), 0.0);
        assert_eq!(f.solidity.get(0, 0, 0), 1.0);
    }
}