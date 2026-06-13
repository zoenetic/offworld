use crate::{DepositionRule, Environment, FieldSet, Generator, Vec3};

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

pub struct World<D> {
    pub environment: Environment,
    pub generator: Generator<D>,
    pub bounds: WorldBounds,
}

impl<D: DepositionRule> World<D> {
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
mod tests {}