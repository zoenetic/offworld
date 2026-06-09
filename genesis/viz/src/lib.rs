use genesis_core::{Field, Vec3};

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

#[cfg(test)]
mod tests {
    use super::*;
    use genesis_core::{Constant, ValueNoise, FieldExt};

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
}