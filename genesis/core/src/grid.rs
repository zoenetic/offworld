use crate::{Field, Vec3};

pub struct Grid<T> {
    pub origin: Vec3,
    pub spacing: f64,
    pub nx: usize,
    pub ny: usize,
    pub nz: usize,
    data: Vec<T>,
}

impl<T: Copy + Default> Grid<T> {
    pub fn new(origin: Vec3, spacing: f64, nx: usize, ny: usize, nz: usize) -> Self {
        Self { origin, spacing, nx, ny, nz, data: vec![T::default(); nx * ny * nz] }
    }
    fn idx(&self, i: usize, j: usize, k: usize) -> usize {
        (k * self.ny + j) * self.nx + i
    }

    pub fn get(&self, i: usize, j: usize, k: usize) -> T { self.data[self.idx(i, j, k)] }

    pub fn set(&mut self, i: usize, j: usize, k: usize, v: T) {
        let n = self.idx(i, j, k);
        self.data[n] = v;
    }

    pub fn as_slice(&self) -> &[T] { &self.data }

    pub fn nearest(&self, p: Vec3) -> T {
        let cell = |w: f64, o: f64, n: usize| {
            (((w - o) / self.spacing).round() as i64).clamp(0, n as i64 -1) as usize
        };
        self.get(
            cell(p.x, self.origin.x, self.nx),
            cell(p.y, self.origin.y, self.ny),
            cell(p.z, self.origin.z, self.nz),
        )
    }
}

pub type FieldGrid = Grid<f64>;

impl Field for FieldGrid {
    fn sample(&self, p: Vec3) -> f64 {
        let to_lattice = |w: f64, o: f64, n: usize| {
            ((w - o) / self.spacing).clamp(0.0, (n - 1) as f64)
        };
        let gx = to_lattice(p.x, self.origin.x, self.nx);
        let gy = to_lattice(p.y, self.origin.y, self.ny);
        let gz = to_lattice(p.z, self.origin.z, self.nz);

        let (i, j, k) = (gx.floor() as usize, gy.floor() as usize, gz.floor() as usize);
        let i1 = (i + 1).min(self.nx - 1);
        let j1 = (j + 1).min(self.ny - 1);
        let k1 = (k + 1).min(self.nz - 1);
        let (fx, fy, fz) = (gx - i as f64, gy - j as f64, gz - k as f64);

        let lerp = |a: f64, b: f64, t: f64| a + t * (b - a);
        let x00 = lerp(self.get(i, j, k),  self.get(i1, j, k),  fx);
        let x10 = lerp(self.get(i, j1, k), self.get(i1, j1, k), fx);
        let x01 = lerp(self.get(i, j, k1), self.get(i1, j, k1), fx);
        let x11 = lerp(self.get(i, j1, k1),self.get(i1, j1, k1),fx);
        lerp(lerp(x00, x10, fy), lerp(x01, x11, fy), fz)
    }
}

pub fn bake(field: &impl Field, origin: Vec3, spacing: f64, nx: usize, ny: usize, nz: usize) -> FieldGrid {
    let mut grid = FieldGrid::new(origin, spacing, nx, ny, nz);
    for k in 0..nz {
        for j in 0..ny {
            for i in 0..nx {
                let p = Vec3::new(
                    origin.x + i as f64 * spacing,
                    origin.y + j as f64 * spacing,
                    origin.z + k as f64 * spacing,
                );
                grid.set(i, j, k, field.sample(p));
            }
        }
    }
    grid
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::{Constant, ValueNoise, FieldExt, MaterialId};

    #[test]
    fn material_grid_defaults_to_none_and_roundtrips() {
        let mut g: Grid<MaterialId> = Grid::new(Vec3::new(0.0, 0.0, 0.0), 1.0, 2, 2, 2);
        assert_eq!(g.get(0, 0, 0), MaterialId::NONE);
        g.set(1, 0, 1, MaterialId(7));
        assert_eq!(g.get(1, 0, 1), MaterialId(7));
        assert_eq!(g.get(0, 0, 0), MaterialId::NONE);
    }

    #[test]
    fn nearest_rounds_to_closest_cell_and_clamps() {
        let mut g: Grid<MaterialId> = Grid::new(Vec3::new(0.0, 0.0, 0.0), 1.0, 3, 1, 1);
        g.set(0, 0, 0, MaterialId(10));
        g.set(1, 0, 0, MaterialId(20));
        g.set(2, 0, 0, MaterialId(30));

        assert_eq!(g.nearest(Vec3::new(1.4, 0.0, 0.0)), MaterialId(20));  // rounds down
        assert_eq!(g.nearest(Vec3::new(1.6, 0.0, 0.0)), MaterialId(30));  // rounds up
        assert_eq!(g.nearest(Vec3::new(0.5, 0.0, 0.0)), MaterialId(20));  // 0.5 rounds away from zero
        assert_eq!(g.nearest(Vec3::new(-5.0, 0.0, 0.0)), MaterialId(10)); // clamps to low edge
        assert_eq!(g.nearest(Vec3::new(99.0, 0.0, 0.0)), MaterialId(30)); // clamps to high edge
    }

    #[test]
    fn baked_constant_is_flat() {
        let g = bake(&Constant(0.25), Vec3::new(0.0, 0.0, 0.0), 1.0, 4, 3, 4);
        assert_eq!(g.sample(Vec3::new(1.5, 2.0, 0.3)), 0.25);
    }

    #[test]
    fn resample_matches_field_at_lattice_points() {
        let f = ValueNoise::new(3).frequency(0.1);
        let g = bake(&f, Vec3::new(0.0, 0.0, 0.0), 1.0, 8, 8, 8);
        let p = Vec3::new(3.0, 2.0, 5.0);
        assert!((g.sample(p) - f.sample(p)).abs() < 1e-12);
    }
}