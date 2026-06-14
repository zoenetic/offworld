mod mesh;

use genesis_core::{Field, Grid, MaterialId, Vec3};

pub use mesh::{mesh_blocky, mesh_smooth, write_ply, Mesh};

pub struct GreyImage {
    pub width: usize,
    pub height: usize,
    pub pixels: Vec<u8>,
}

pub fn render_field_slice(field: &impl Field, width: usize, height: usize, scale: f64) -> GreyImage {
    let mut pixels = vec![0u8; width * height];
    for j in 0..height {
        for i in 0..width {
            let p = Vec3::new(i as f64 * scale, 0.0, j as f64 * scale);
            let v = field.sample(p).clamp(0.0, 1.0);
            pixels[j * width + i] = (v * 255.0) as u8;
        }
    }
    GreyImage { width, height, pixels }
}

pub fn render_heightmap(height: &[f64], w: usize, h: usize) -> GreyImage {
    let (lo, hi) = height.iter().fold((f64::MAX, f64::MIN), |(lo, hi), &v| (lo.min(v), hi.max(v)));
    let range = (hi - lo).max(1e-9);
    let pixels = height.iter().map(|&v| (((v - lo) / range) * 255.0) as u8).collect();
    GreyImage { width: w, height: h, pixels }
}

pub fn render_vertical_slice(field: &impl Field, width: usize, height: usize, scale: f64, z: f64) -> GreyImage {
    let mut pixels = vec![0u8; width * height];
    for j in 0..height {
        for i in 0..width {
            let y = (height - 1 - j) as f64 * scale;
            let p = Vec3::new(i as f64 * scale, y, z);
            let v = field.sample(p).clamp(0.0, 1.0);
            pixels[j * width + i] = (v * 255.0) as u8;
        }
    }
    GreyImage { width, height, pixels }
}

pub fn write_pgm(img: &GreyImage, path: &str) -> std::io::Result<()> {
    use std::io::Write;
    let mut f = std::io::BufWriter::new(std::fs::File::create(path)?);
    write!(f, "P5\n{} {}\n255\n", img.width, img.height)?;
    f.write_all(&img.pixels)?;
    Ok(())
}

pub struct ColourImage {
    pub width: usize,
    pub height: usize,
    pub pixels: Vec<u8>,
}

pub fn write_ppm(img: &ColourImage, path: &str) -> std::io::Result<()> {
    use std::io::Write;
    let mut f = std::io::BufWriter::new(std::fs::File::create(path)?);
    write!(f, "P6\n{} {}\n255\n", img.width, img.height)?;
    f.write_all(&img.pixels)?;
    Ok(())
}

pub fn render_material_slice(
    material: &Grid<MaterialId>,
    palette: impl Fn(MaterialId) -> [u8; 3],
    width: usize,
    height: usize,
    scale: f64,
    z: f64,
) -> ColourImage {
    let mut pixels = vec![0u8; width * height * 3];
    for j in 0..height {
        for i in 0..width {
            let y = (height - 1 - j) as f64 * scale;
            let m = material.nearest(Vec3::new(i as f64 * scale, y, z));
            let [r, g, b] = palette(m);
            let idx = (j * width + i) * 3;
            pixels[idx] = r;
            pixels[idx + 1] = g;
            pixels[idx + 2] = b;
        }
    }
    ColourImage { width, height, pixels }
}

#[cfg(test)]
mod tests {
    use super::*;
    use genesis_core::{Constant, FieldExt, ValueNoise};

    #[test]
    fn renders_expected_dimensions() {
        let img = render_field_slice(&Constant(0.5), 16, 8, 1.0);
        assert_eq!(img.width, 16);
        assert_eq!(img.height, 8);
        assert_eq!(img.pixels.len(), 16 * 8);
        assert!(img.pixels.iter().all(|&b| b == 127));
    }

    #[test]
    fn render_is_deterministic() {
        let f = ValueNoise::new(1).frequency(0.05);
        let a = render_field_slice(&f, 32, 32, 1.0);
        let b = render_field_slice(&f, 32, 32, 1.0);
        assert_eq!(a.pixels, b.pixels);
    }

    #[test]
    #[ignore = "writes slice-*.pgm for visual comparison"]
    fn dump_axis_bias_slices() -> std::io::Result<()> {
        let at = |offset: Vec3| ValueNoise::new(1).frequency(1.0 / 24.0).translate(offset);

        let img = render_field_slice(&at(Vec3::new(-256.0, 0.0, -256.0)), 512, 512, 1.0);
        write_pgm(&img, "slice-origin.pgm")?;

        let img = render_field_slice(&at(Vec3::new(10_000.0, 0.0, 10_000.0)), 512, 512, 1.0);
        write_pgm(&img, "slice-far.pgm")?;

        let img = render_field_slice(&at(Vec3::new(0.0, 10_000.0, 0.0)), 512, 512, 1.0);
        write_pgm(&img, "slice-y-high.pgm")?;

        Ok(())
    }
}