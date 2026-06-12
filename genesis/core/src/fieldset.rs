use crate::{Grid, MaterialId, Vec3};

pub struct FieldSet {
    pub solidity: Grid<f64>,
    pub material: Grid<MaterialId>,
}

impl FieldSet {
    pub fn new(origin: Vec3, spacing: f64, nx: usize, ny: usize, nz: usize) -> Self {
        Self {
            solidity: Grid::new(origin, spacing, nx, ny, nz),
            material: Grid::new(origin, spacing, nx, ny, nz),
        }
    }
}