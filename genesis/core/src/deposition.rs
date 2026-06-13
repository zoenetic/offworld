use crate::{Environment, EnvironmentSample, FieldId, FieldSet, Fixed, MaterialId, MaterialSelector, Vec3};

pub struct Deposit {
    pub solidity: f64,
    pub material: MaterialId,
}

pub fn deposit_region<R: DepositionRule>(
    env: &Environment, rule: &R,
    origin: Vec3, spacing: f64, nx: usize, ny: usize, nz: usize,
) -> FieldSet {
    let mut fields = FieldSet::new(origin, spacing, nx, ny, nz);
    for k in 0..nz {
        for i in 0..nx {
            let x = origin.x + i as f64 * spacing;
            let z = origin.z + k as f64 * spacing;
            let col = rule.begin_column(env, x, z);
            for j in 0..ny {
                let d = rule.deposit(&col, j as f64 * spacing, spacing);
                fields.solidity.set(i, j, k, d.solidity);
                fields.material.set(i, j, k, d.material);
            }
        }
    }
    fields
}

pub trait DepositionRule {
    type Column;
    fn begin_column(&self, env: &Environment, x: f64, y: f64) -> Self::Column;
    fn deposit(&self, column: &Self::Column, y: f64, cell_height: f64) -> Deposit;
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

pub struct ColumnPlan {
    sample: EnvironmentSample,
    bed_tops: Vec<f64>,
    mantle_t: f64,
    total: f64,
    warp: f64,
}

pub struct LayeredDeposition {
    pub beds: Vec<Layer>,
    pub mantle: Layer,
    pub landform: FieldId,
    pub tectonic: FieldId,
    pub sea_level_temp: f64,
    pub lapse_rate: f64,
    pub moisture: FieldId,
}

impl LayeredDeposition {
    pub fn surface_height(&self, env: &Environment, x: f64, z: f64) -> f64 {
        let b_t: f64 = self.beds.iter()
            .map(|b| env.sample(b.thickness, Vec3::new(x, 0.0, z)))
            .sum();
        let m_t = env.sample(self.mantle.thickness, Vec3::new(x, 0.0, z));
        b_t + m_t
    }

    fn environment_sample(&self, env: &Environment, x: f64, z: f64) -> EnvironmentSample {
        let e = 4.0;
        let s = |a: f64, b: f64| env.sample(self.landform, Vec3::new(a, 0.0, b));
        let hx = (s(x + e, z) - s(x - e, z)) / (2.0 * e);
        let hz = (s(x, z + e) - s(x, z - e)) / (2.0 * e);
        let elevation = s(x, z);
        EnvironmentSample {
            slope: (hx * hx + hz * hz).sqrt(),
            elevation,
            temperature: self.sea_level_temp - self.lapse_rate * elevation,
            moisture: env.sample(self.moisture, Vec3::new(x, 0.0, z)),
        }
    }
}

impl DepositionRule for LayeredDeposition {
    type Column = ColumnPlan;

    fn begin_column(&self, env: &Environment, x: f64, z: f64) -> ColumnPlan {
        let p = Vec3::new(x, 0.0, z);
        let mut bed_tops = Vec::with_capacity(self.beds.len());
        let mut acc = 0.0;
        for bed in &self.beds {
            acc += env.sample(bed.thickness, p);
            bed_tops.push(acc);
        }
        let mantle_t = env.sample(self.mantle.thickness, p);
        ColumnPlan {
            sample: self.environment_sample(env, x, z),
            warp: env.sample(self.tectonic, p),
            total: acc + mantle_t,
            mantle_t,
            bed_tops,
        }
    }

    fn deposit(&self, col: &ColumnPlan, y: f64, cell_height: f64) -> Deposit {
        let remaining = col.total - y;
        if remaining <= 0.0 {
            return Deposit { solidity: 0.0, material: MaterialId::NONE };
        }
        let selector: &dyn MaterialSelector = if remaining <= col.mantle_t {
            self.mantle.material.as_ref()
        } else {
            let probe = y - col.warp;
            let band = col.bed_tops.iter().position(|&t| probe < t).unwrap_or(self.beds.len() - 1);
            self.beds[band].material.as_ref()
        };
        Deposit {
            solidity: (remaining / cell_height).min(1.0),
            material: selector.select(&col.sample, remaining),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::Constant;

    #[test]
    fn layers_stack_bottom_to_top() {
        let mut env = Environment::new();
        let t = env.add(Constant(3.0));
        let landform = env.add(Constant(1.0));
        let tectonic = env.add(Constant(1.0));
        let rule = LayeredDeposition {
            beds: vec![
                Layer::fixed(MaterialId(1), t),
                Layer::fixed(MaterialId(2), t),
            ],
            mantle: Layer::fixed(MaterialId(3), t),
            landform,
            tectonic,
            sea_level_temp: 20.0,
            lapse_rate: 0.2,
            moisture: env.add(Constant(1.0)),
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