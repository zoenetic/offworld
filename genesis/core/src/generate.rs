use crate::{deposit_region, DepositionRule, Environment, FieldGrid, Vec3};

pub trait Erosion {
    fn erode(&self, grid: &mut FieldGrid, env: &Environment);
}

pub struct NoErosion;

impl Erosion for NoErosion {
    fn erode(&self, _grid: &mut FieldGrid, _env: &Environment) {}
}

pub trait Carver {
    fn carve(&self, grid: &mut FieldGrid, env: &Environment);
}

pub struct NoCarve;

impl Carver for NoCarve {
    fn carve(&self, _grid: &mut FieldGrid, _env: &Environment) {}
}

pub struct Generator {
    deposition: Box<dyn DepositionRule>,
    erosion: Box<dyn Erosion>,
    carver: Box<dyn Carver>,
}

impl Generator {
    pub fn new(deposition: impl DepositionRule + 'static) -> Self {
        Self {
            deposition: Box::new(deposition),
            erosion: Box::new(NoErosion),
            carver: Box::new(NoCarve),
        }
    }

    pub fn with_erosion(mut self, erosion: impl Erosion + 'static) -> Self {
        self.erosion = Box::new(erosion);
        self
    }

    pub fn with_carver(mut self, carver: impl Carver + 'static) -> Self {
        self.carver = Box::new(carver);
        self
    }

    pub fn generate(&self, env: &Environment, origin: Vec3, spacing: f64, nx: usize, ny: usize, nz: usize) -> FieldGrid {
        let mut grid = deposit_region(env, &*self.deposition, origin, spacing, nx, ny, nz);
        self.erosion.erode(&mut grid, env);
        self.carver.carve(&mut grid, env);
        grid
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::{Accrete, Constant};

    #[test]
    fn defaults_match_bare_deposition() {
        let mut env = Environment::new();
        let thickness = env.add(Constant(5.0));
        let generator = Generator::new(Accrete { thickness });
        let grid = generator.generate(&env, Vec3::new(0.0, 0.0, 0.0), 1.0, 1, 10, 1);
        assert_eq!(grid.get(0, 4, 0), 1.0);
        assert_eq!(grid.get(0, 5, 0), 0.0);
    }
}