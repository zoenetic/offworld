use crate::{Environment, Erosion, FieldSet, MaterialId};

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