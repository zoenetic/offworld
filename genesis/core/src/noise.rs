use crate::{Field, Vec3};

pub struct ValueNoise {
    pub seed: u64,
}

impl ValueNoise {
    pub const fn new(seed: u64) -> Self {
        Self { seed }
    }
}

impl Field for ValueNoise {
    fn sample(&self, p: Vec3) -> f64 {
        let (x0, y0, z0) = (p.x.floor(), p.y.floor(), p.z.floor());
        let (xi, yi, zi) = (x0 as i32, y0 as i32, z0 as i32);
        let (fx, fy, fz) = (p.x - x0, p.y - y0, p.z - z0);

        let (u, v, w) = (fade(fx), fade(fy), fade(fz));

        let c = |dx, dy, dz| to_unit(hash(xi + dx, yi + dy, zi + dz, self.seed));

        let x00 = lerp(c(0, 0, 0), c(1, 0, 0), u);
        let x10 = lerp(c(0, 1, 0), c(1, 1, 0), u);
        let x01 = lerp(c(0, 0, 1), c(1, 0, 1), u);
        let x11 = lerp(c(0, 1, 1), c(1, 1, 1), u);
        let y0v = lerp(x00, x10, v);
        let y1v = lerp(x01, x11, v);
        lerp(y0v, y1v, w)
    }
}

fn hash(x: i32, y: i32, z: i32, seed: u64) -> u64 {
    let mut h = seed;
    h ^= (x as u32 as u64).wrapping_mul(0x9E37_79B9_7F4A_7C15);
    h ^= (y as u32 as u64).wrapping_mul(0xC2B2_AE3D_27D4_EB4F);
    h ^= (z as u32 as u64).wrapping_mul(0x1656_67B1_9E37_79F9);
    h = (h ^ (h >> 30)).wrapping_mul(0xBF58_476D_1CE4_E5B9);
    h = (h ^ (h >> 27)).wrapping_mul(0x94D0_49BB_1331_11EB);
    h ^ (h >> 31)
}

fn to_unit(h: u64) -> f64 {
    (h >> 11) as f64 / (1u64 << 53) as f64
}

fn fade(t: f64) -> f64 {
    t * t * t * (t * (t * 6.0 - 15.0) + 10.0)
}

fn lerp(a: f64, b: f64, t: f64) -> f64 {
    a + t * (b - a)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn is_deterministic() {
        let n = ValueNoise::new(42);
        let p = Vec3::new(1.5, -2.25, 3.75);
        assert_eq!(n.sample(p), n.sample(p));
    }

    #[test]
    fn is_within_unit_range() {
        let n = ValueNoise::new(7);
        for i in 0..2000 {
            let f = i as f64 * 0.137;
            let v = n.sample(Vec3::new(f, -f * 0.5, f * 0.25));
            assert!((0.0..1.0).contains(&v), "out of range: {v}");
        }
    }

    #[test]
    fn seed_changes_output() {
        let p = Vec3::new(0.3, 0.7, 0.1);
        assert_ne!(ValueNoise::new(1).sample(p), ValueNoise::new(2).sample(p));
    }

    #[test]
    fn hash_has_no_structured_collisions() {
        use std::collections::HashMap;

        let seed = 42;
        let mut seen: HashMap<u64, (i32, i32, i32)> = HashMap::new();
        let mut check = |x: i32, y: i32, z: i32| {
            let h = hash(x, y, z, seed);
            if let Some(&prev) = seen.get(&h) {
                assert_eq!(prev, (x, y, z), "collision: {prev:?} vs {:?}", (x, y, z));
            }
            seen.insert(h, (x, y, z));
        };

        for x in -256..256 {
            for z in -256..256 {
                check(x, 0, z);
            }
        }

        for x in -32..32 {
            for y in -32..32 {
                for z in -32..32 {
                    check(x, y, z);
                }
            }
        };

        for x in 10_000..10_064 {
            for y in 10_000..10_064 {
                for z in 10_000..10_064 {
                    check(x, y, z);
                }
            }
        }
    }

    #[test]
    fn hash_low_tail_is_window_independent() {
        let frac_low = |x0: i32, z0: i32| {
            let mut low = 0u32;
            for x in x0..x0 + 1024 {
                for z in z0..z0 + 1024 {
                    if to_unit(hash(x, 0, z, 42)) < 0.1 {
                        low += 1;
                    }
                }
            }
            low as f64 / (1024.0 * 1024.0)
        };
        let origin = frac_low(-512, -512);
        let far = frac_low(10_000, 10_000);
        assert!((origin - 0.1).abs() < 1.5e-3, "origin low tail: {origin}");
        assert!((far - 0.1).abs() < 1.5e-3, "far low tail: {far}");
    }
}