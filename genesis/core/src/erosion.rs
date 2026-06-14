use crate::{Environment, FieldSet, MaterialId, Rng};

pub trait Erosion {
    fn erode(&self, fields: &mut FieldSet, env: &Environment);
}

pub struct HydraulicErosion {
    pub seed: u64,
    pub droplets: u32,
    pub inertia: f64,
    pub capacity: f64,
    pub min_slope: f64,
    pub erode_rate: f64,
    pub deposit_rate: f64,
    pub evaporation: f64,
    pub gravity: f64,
    pub max_lifetime: u32,
    pub sediment: MaterialId,
    pub erode_radius: i32,
    pub scale: usize,
}

impl HydraulicErosion {
    pub fn simulate_droplets(&self, height: &mut [f64], nx: usize, nz: usize) {
        let idx = |i: usize, k: usize| k * nx + i;
        let brush = make_brush(self.erode_radius);
        let mut rng = Rng(self.seed);
        for _ in 0..self.droplets {
            let mut px = rng.unit() * (nx - 1) as f64;
            let mut pz = rng.unit() * (nz - 1) as f64;
            let (mut dx, mut dz) = (0.0f64, 0.0f64);
            let (mut speed, mut water, mut sediment) = (1.0f64, 1.0f64, 0.0f64);

            for _ in 0..self.max_lifetime {
                let (cx, cz) = (px.floor() as usize, pz.floor() as usize);
                let (fx, fz) = (px - cx as f64, pz - cz as f64);

                let (nw, ne, sw, se) = (
                    height[idx(cx, cz)], height[idx(cx + 1, cz)],
                    height[idx(cx, cz + 1)], height[idx(cx + 1, cz + 1)],
                );
                let h_old = nw * (1.0 - fx) * (1.0 - fz) + ne * fx * (1.0 - fz) + sw * (1.0 - fx) * fz + se * fx * fz;
                let gx = (ne - nw) * (1.0 - fz) + (se - sw) * fz;
                let gz = (sw - nw) * (1.0 - fx) + (se - ne) * fx;

                dx = dx * self.inertia - gx * (1.0 - self.inertia);
                dz = dz * self.inertia - gz * (1.0 - self.inertia);
                let len = (dx * dx + dz * dz).sqrt();
                if len < 1e-9 { break; }
                dx /= len;
                dz /= len;
                px += dx;
                pz += dz;
                if px < 0.0 || pz < 0.0 || px >= (nx - 1) as f64 || pz >= (nz - 1) as f64 { break; }

                let (ncx, ncz) = (px.floor() as usize, pz.floor() as usize);
                let (nfx, nfz) = (px - ncx as f64, pz - ncz as f64);
                let h_new = {
                    let (nw, ne, sw, se) = (
                        height[idx(ncx, ncz)], height[idx(ncx + 1, ncz)],
                        height[idx(ncx, ncz + 1)], height[idx(ncx + 1, ncz + 1)],
                    );
                    nw * (1.0 - nfx) * (1.0 - nfz) + ne * nfx * (1.0 - nfz) + sw * (1.0 - nfx) * nfz + se * nfx * nfz
                };
                let delta = h_new - h_old;

                let cap = (-delta).max(self.min_slope) * speed * water * self.capacity;

                let mut change = |amount: f64| {
                    height[idx(cx, cz)] += amount * (1.0 - fx) * (1.0 - fz);
                    height[idx(cx + 1, cz)] += amount * fx * (1.0 - fz);
                    height[idx(cx, cz + 1)] += amount * (1.0 - fx) * fz;
                    height[idx(cx + 1, cz + 1)] += amount * fx * fz;
                };

                if sediment > cap || delta > 0.0 {
                    let drop = if delta > 0.0 { delta.min(sediment) } else { (sediment - cap) * self.deposit_rate };
                    sediment -= drop;
                    change(drop);
                } else {
                    let dig = ((cap - sediment) * self.erode_rate).min(-delta);
                    sediment += dig;
                    for &(bx, bz, w) in &brush {
                        let bi = cx as i64 + bx as i64;
                        let bk = cz as i64 + bz as i64;
                        if bi >= 0 && bk >= 0 && (bi as usize) < nx && (bk as usize) < nz {
                            height[(bk as usize) * nx + (bi as usize)] -= dig * w;
                        }
                    }
                }

                speed = (speed * speed - delta * self.gravity).max(0.0).sqrt();
                water *= 1.0 - self.evaporation;
                if water < 1e-4 { break; }
            }
        }
    }
}

impl Erosion for HydraulicErosion {
    fn erode(&self, fields: &mut FieldSet, _env: &Environment) {
        let nx = fields.solidity.nx;
        let nz = fields.solidity.nz;
        let mut fine = extract_heightmap(fields);
        let f = self.scale.max(1);

        if f == 1 {
            self.simulate_droplets(&mut fine, nx, nz);
        } else {
            let (cnx, cnz) = (nx / f, nz / f);
            let mut coarse = vec![0.0; cnx * cnz];
            for ck in 0..cnz {
                for ci in 0..cnx {
                    let mut sum = 0.0;
                    for dz in 0..f {
                        for dx in 0..f {
                            sum += fine[(ck * f + dz) * nx + (ci * f + dx)];
                        }
                    }
                    coarse[ck * cnx + ci] = sum / (f * f) as f64;
                }
            }

            let before = coarse.clone();
            self.simulate_droplets(&mut coarse, cnx, cnz);

            for k in 0..nz {
                for i in 0..nx {
                    fine[k * nx + i] += sample_delta(&coarse, &before, cnx, cnz, i as f64 / f as f64, k as f64 / f as f64);
                }
            }
        }

        reimpose_heightmap(fields, &fine, self.sediment);
    }
}

pub struct ThermalErosion {
    pub iterations: u32,
    pub talus: f64,
    pub rate: f64,
    pub sediment: MaterialId,
}

impl Erosion for ThermalErosion {
    fn erode(&self, fields: &mut FieldSet, _env: &Environment) {
        let (nx, ny, nz) = (fields.solidity.nx, fields.solidity.ny, fields.solidity.nz);
        let h = |i: usize, k: usize| k * nx + i;
        let mut height = vec![0.0f64; nx * nz];
        for k in 0..nz {
            for i in 0..nx {
                let mut sum = 0.0;
                for j in 0..ny {
                    sum += fields.solidity.get(i, j, k);
                }
                height[h(i, k)] = sum;
            }
        }

        const NEIGHBOURS: [(i64, i64); 4] = [(-1, 0), (1, 0), (0, -1), (0, 1)];

        for _ in 0..self.iterations {
            let mut delta = vec![0.0f64; nx * nz];
            for k in 0..nz {
                for i in 0..nx {
                    let hc = height[h(i, k)];
                    let mut excess = [0.0f64; 4];
                    let mut total = 0.0;
                    let mut steepest = 0.0f64;
                    for (n, (di, dk)) in NEIGHBOURS.iter().enumerate() {
                        let (ni, nk) = (i as i64 + di, k as i64 + dk);
                        if ni < 0 || nk < 0 || ni as usize >= nx || nk as usize >= nz {
                            continue;
                        }
                        let e = hc - height[h(ni as usize, nk as usize)] - self.talus;
                        if e > 0.0 {
                            excess[n] = e;
                            total += e;
                            steepest = steepest.max(e);
                        }
                    }
                    if total <= 0.0 {
                        continue;
                    }
                    let moved = self.rate * steepest;
                    for (n, (di, dk)) in NEIGHBOURS.iter().enumerate() {
                        if excess[n] <= 0.0 {
                            continue;
                        }
                        let (ni, nk) = ((i as i64 + di) as usize, (k as i64 + dk) as usize);
                        delta[h(ni, nk)] += moved * (excess[n] / total);
                    }
                    delta[h(i, k)] -= moved;
                }
            }
            for x in 0..nx * nz {
                height[x] = (height[x] + delta[x]).max(0.0);
            }
        }

        for k in 0..nz {
            for i in 0..nx {
                let col = height[h(i, k)];
                for j in 0..ny {
                    let sol = (col - j as f64).clamp(0.0, 1.0);
                    fields.solidity.set(i, j, k, sol);
                    if sol == 0.0 {
                        fields.material.set(i, j, k, MaterialId::NONE);
                    } else if fields.material.get(i, j, k) == MaterialId::NONE {
                        fields.material.set(i, j, k, self.sediment);
                    }
                }
            }
        }
    }
}

pub fn extract_heightmap(fields: &FieldSet) -> Vec<f64> {
    let (nx, ny, nz) = (fields.solidity.nx, fields.solidity.ny, fields.solidity.nz);
    let mut height = vec![0.0; nx * nz];
    for k in 0..nz {
        for i in 0..nx {
            height[k * nx + i] = (0..ny).map(|j| fields.solidity.get(i, j, k)).sum();
        }
    }
    height
}

fn make_brush(radius: i32) -> Vec<(i32, i32, f64)> {
    let mut brush = Vec::new();
    let mut total = 0.0;
    for dz in -radius..=radius {
        for dx in -radius..=radius {
            let dist = ((dx * dx + dz * dz) as f64).sqrt();
            if dist <= radius as f64 {
                let w = 1.0 - dist / (radius as f64 + 1.0);
                brush.push((dx, dz, w));
                total += w;
            }
        }
    }
    for b in brush.iter_mut() {
        b.2 /= total;
    }
    brush
}

pub fn reimpose_heightmap(fields: &mut FieldSet, height: &[f64], sediment: MaterialId) {
    let (nx, ny, nz) = (fields.solidity.nx, fields.solidity.ny, fields.solidity.nz);
    for k in 0..nz {
        for i in 0..nx {
            let col = height[k * nx + i];
            for j in 0..ny {
                let sol = (col - j as f64).clamp(0.0, 1.0);
                fields.solidity.set(i, j, k, sol);
                if sol == 0.0 {
                    fields.material.set(i, j, k, MaterialId::NONE);
                } else if fields.material.get(i, j, k) == MaterialId::NONE {
                    fields.material.set(i, j, k, sediment);
                }
            }
        }
    }
}

fn sample_delta(after: &[f64], before: &[f64], cnx: usize, cnz: usize, cx: f64, cz: f64) -> f64 {
    let x0 = (cx.floor() as usize).min(cnx - 1);
    let z0 = (cz.floor() as usize).min(cnz - 1);
    let x1 = (x0 + 1).min(cnx - 1);
    let z1 = (z0 + 1).min(cnz - 1);
    let (fx, fz) = (cx - x0 as f64, cz - z0 as f64);
    let d = |i: usize, k: usize| after[k * cnx + i] - before[k * cnx + i];
    let top = d(x0, z0) * (1.0 - fx) + d(x1, z0) * fx;
    let bot = d(x0, z1) * (1.0 - fx) + d(x1, z1) * fx;
    top * (1.0 - fz) + bot * fz
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::Vec3;

    fn height(f: &FieldSet, i: usize, k: usize) -> f64 {
        (0..f.solidity.ny).map(|j| f.solidity.get(i, j, k)).sum()
    }

    #[test]
    fn thermal_relaxes_a_spike() {
        let mut f = FieldSet::new(Vec3::new(0.0, 0.0, 0.0), 1.0, 3, 12, 1);
        for j in 0..8 {
            f.solidity.set(1, j, 0, 1.0);
        }
        let before = height(&f, 1, 0);
        ThermalErosion { iterations: 60, talus: 1.0, rate: 0.2, sediment: MaterialId(4) }
            .erode(&mut f, &Environment::new());
        assert!(height(&f, 1, 0) < before, "spike should lower");
        assert!(height(&f, 0, 0) > 0.0, "material should spread to the neighbour");
    }
}