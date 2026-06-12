use crate::{Environment, FieldSet, FieldId, MaterialId, Vec3};

pub struct Deposit {
    pub solidity: f64,
    pub material: MaterialId,
}

#[derive(Default)]
pub struct ColumnState {
    pub deposited: f64,
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
            return Deposit { solidity: 0.0, material: MaterialId::NONE }
        }
        let solidity = (remaining / cell_height).min(1.0);
        state.deposited += cell_height;
        Deposit { solidity, material: self.material }
    }
}

pub struct Layer {
    pub material: MaterialId,
    pub thickness: FieldId,
}

pub struct LayeredDeposition {
    pub layers: Vec<Layer>,
}

impl DepositionRule for LayeredDeposition {
    fn deposit(&self, env: &Environment, p: Vec3, cell_height: f64, state: &mut ColumnState) -> Deposit {
        let col = Vec3::new(p.x, 0.0, p.z);
        let mut base = 0.0;
        let mut material = MaterialId::NONE;
        let mut found = false;
        for layer in &self.layers {
            let top = base + env.sample(layer.thickness, col);
            if !found && state.deposited < top {
                material = layer.material;
                found = true;
            }
            base = top;
        }
        let total = base;
        let remaining = total - state.deposited;
        if remaining <= 0.0 {
            return Deposit { solidity: 0.0, material: MaterialId::NONE }
        }
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
            return Deposit { solidity: 0.0, material: MaterialId::NONE }
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
        let rule = LayeredDeposition { layers: vec![
            Layer{ material: MaterialId(1), thickness: t },
            Layer{ material: MaterialId(2), thickness: t },
            Layer{ material: MaterialId(3), thickness: t },
        ]};
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