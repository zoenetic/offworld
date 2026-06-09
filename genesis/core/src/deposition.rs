use crate::{Environment, FieldGrid, FieldId, Vec3};

#[derive(Default)]
pub struct ColumnState {
    pub deposited: f64,
}

pub trait DepositionRule {
    fn deposit(&self, env: &Environment, p: Vec3, cell_height: f64, state: &mut ColumnState) -> f64;
}

pub struct Accrete {
    pub thickness: FieldId,
}

impl DepositionRule for Accrete {
    fn deposit(&self, env: &Environment, p: Vec3, cell_height: f64, state: &mut ColumnState) -> f64 {
        let target = env.sample(self.thickness, Vec3::new(p.x, 0.0, p.z));
        if state.deposited < target {
            state.deposited += cell_height;
            1.0
        } else {
            0.0
        }
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
) -> FieldGrid {
    let mut grid = FieldGrid::new(origin, spacing, nx, ny, nz);
    for k in 0..nz {
        for i in 0..nx {
            let mut state = ColumnState::default();
            for j in 0..ny {
                let p = Vec3::new(
                    origin.x + i as f64 * spacing,
                    origin.y + j as f64 * spacing,
                    origin.z + k as f64 * spacing,
                );
                let solidity = rule.deposit(env, p, spacing, &mut state);
                grid.set(i, j, k, solidity);
            }
        }
    }
    grid
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::Constant;

    #[test]
    fn accretes_to_target_height() {
        let mut env = Environment::new();
        let thickness = env.add(Constant(5.0));
        let rule = Accrete { thickness };
        let grid = deposit_region(&env, &rule, Vec3::new(0.0, 0.0, 0.0), 1.0, 1, 10, 1);
        for j in 0..10 {
            let want = if j < 5 { 1.0 } else { 0.0 };
            assert_eq!(grid.get(0, j, 0), want, "cell {j}");
        }
    }
}