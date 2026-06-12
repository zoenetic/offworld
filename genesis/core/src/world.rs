use crate::{Environment, FieldSet, Generator, Vec3};

pub struct WorldBounds {
    pub min_y: f64,
    pub max_y: f64,
}

impl WorldBounds {
    pub fn height(&self) -> f64 {
        self.max_y - self.min_y
    }
}

pub struct Region {
    pub min_x: f64,
    pub min_z: f64,
    pub spacing: f64,
    pub nx: usize,
    pub nz: usize,
}

pub struct World {
    pub environment: Environment,
    pub generator: Generator,
    pub bounds: WorldBounds,
}

impl World {
    pub fn generate(&self, region: &Region) -> FieldSet {
        let ny = (self.bounds.height() / region.spacing).ceil() as usize;
        let origin = Vec3::new(region.min_x, self.bounds.min_y, region.min_z);
        self.generator.generate(
            &self.environment,
            origin,
            region.spacing,
            region.nx,
            ny,
            region.nz,
        )
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::{Accrete, Constant, MaterialId};

    #[test]
    fn generate_derives_height_from_bounds() {
        let mut env = Environment::new();
        let thickness = env.add(Constant(5.0));
        let world = World {
            environment: env,
            generator: Generator::new(Accrete { thickness, material: MaterialId(1) }),
            bounds: WorldBounds { min_y: 0.0, max_y: 10.0 },
        };
        let fields = world.generate(&Region { min_x: 0.0, min_z: 0.0, spacing: 1.0, nx: 1, nz: 1});
        assert_eq!(fields.solidity.ny, 10);
        assert_eq!(fields.solidity.get(0, 4, 0), 1.0);
        assert_eq!(fields.solidity.get(0, 5, 0), 0.0);
    }
}