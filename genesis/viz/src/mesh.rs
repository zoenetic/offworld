use genesis_core::{MaterialId, Grid, Vec3};

pub struct Vertex {
    pub pos: [f32; 3],
    pub normal: [f32; 3],
    pub colour: [u8; 3],
}

pub struct Mesh {
    pub vertices: Vec<Vertex>,
    pub triangles: Vec<[u32; 3]>,
}

impl Mesh {
    fn quad(&mut self, corners: [[f32; 3]; 4], normal: [f32; 3], colour: [u8; 3]) {
        let base = self.vertices.len() as u32;
        for c in corners {
            self.vertices.push(Vertex { pos: c, normal, colour})
        }
        self.triangles.push([base, base + 1, base + 2]);
        self.triangles.push([base, base + 2, base + 3]);
    }
}

const FACES: [([i64; 3], [[f32; 3]; 4]); 6] = [
    ([-1, 0, 0], [[0.0,0.0,0.0],[0.0,0.0,1.0],[0.0,1.0,1.0],[0.0,1.0,0.0]]),
    ([ 1, 0, 0], [[1.0,0.0,0.0],[1.0,1.0,0.0],[1.0,1.0,1.0],[1.0,0.0,1.0]]),
    ([ 0,-1, 0], [[0.0,0.0,0.0],[1.0,0.0,0.0],[1.0,0.0,1.0],[0.0,0.0,1.0]]),
    ([ 0, 1, 0], [[0.0,1.0,0.0],[0.0,1.0,1.0],[1.0,1.0,1.0],[1.0,1.0,0.0]]),
    ([ 0, 0,-1], [[0.0,0.0,0.0],[0.0,1.0,0.0],[1.0,1.0,0.0],[1.0,0.0,0.0]]),
    ([ 0, 0, 1], [[0.0,0.0,1.0],[1.0,0.0,1.0],[1.0,1.0,1.0],[0.0,1.0,1.0]]),
];

pub fn mesh_blocky(
    solidity: &Grid<f64>,
    material: &Grid<MaterialId>,
    iso: f64,
    palette: impl Fn(MaterialId) -> [u8; 3],
) -> Mesh {
    let mut mesh = Mesh { vertices: Vec::new(), triangles: Vec::new() };
    let solid = |i: i64, j: i64, k: i64| {
        i >= 0 && j >= 0 && k >= 0
            && (i as usize) < solidity.nx && (j as usize) < solidity.ny && (k as usize) < solidity.nz
            && solidity.get(i as usize, j as usize, k as usize) >= iso
    };
    let s = solidity.spacing as f32;
    let (ox, oy, oz) = (solidity.origin.x as f32, solidity.origin.y as f32, solidity.origin.z as f32);

    for k in 0..solidity.nz {
        for j in 0..solidity.ny {
            for i in 0..solidity.nx {
                if !solid(i as i64, j as i64, k as i64) {
                    continue;
                }
                let colour = palette(material.get(i, j, k));
                let base = [ox + i as f32 * s, oy + j as f32 * s, oz + k as f32 * s];
                for (off, corners) in FACES {
                    if solid(i as i64 + off[0], j as i64 + off[1], k as i64 + off[2]) {
                        continue;
                    }
                    let verts = corners.map(|c| [base[0] + c[0]*s, base[1] + c[1]*s, base[2] + c[2]*s]);
                    let normal = [off[0] as f32, off[1] as f32, off[2] as f32];
                    mesh.quad(verts, normal, colour);
                }
            }
        }
    }
    mesh
}

pub fn mesh_smooth(
    solidity: &Grid<f64>,
    material: &Grid<MaterialId>,
    iso: f64,
    palette: impl Fn(MaterialId) -> [u8; 3],
) -> Mesh {
    let (nx, ny, nz) = (solidity.nx, solidity.ny, solidity.nz);
    let s = solidity.spacing;
    let origin = solidity.origin;
    let val = |i: usize, j: usize, k: usize| solidity.get(i, j, k);

    const C: [[usize; 3]; 8] = [
        [0,0,0],[1,0,0],[1,1,0],[0,1,0],[0,0,1],[1,0,1],[1,1,1],[0,1,1],
    ];
    const E: [[usize; 2]; 12] = [
        [0,1],[1,2],[2,3],[3,0], [4,5],[5,6],[6,7],[7,4], [0,4],[1,5],[2,6],[3,7],
    ];

    let cidx = |i, j, k| (k * (ny -1) +j) * (nx - 1) + i;
    let mut cell_vert = vec![u32::MAX; (nx -1) * (ny - 1) * (nz -1)];
    let mut mesh = Mesh { vertices: Vec::new(), triangles: Vec::new() };

    for k in 0..nz -1 {
        for j in 0..ny -1 {
            for i in 0..nx -1 {
                let cv: [f64; 8] = std::array::from_fn(|c| val(i + C[c][0], j + C[c][1], k + C[c][2]));
                let inside = cv.map(|v| v >= iso);
                let n = inside.iter().filter(|&&b| b).count();
                if n == 0 || n == 8 {
                    continue;
                }
                let (mut sum, mut count) = ([0.0f64; 3], 0.0);
                for [a, b] in E {
                    if inside[a] != inside[b] {
                        let t = (iso - cv[a]) / (cv[b] - cv[a]);
                        for d in 0..3 {
                            sum[d] += C[a][d] as f64 + t * (C[b][d] as f64 - C[a][d] as f64);
                        }
                        count += 1.0;
                    }
                }
                let p = [
                    origin.x + (i as f64 + sum[0] / count) * s,
                    origin.y + (j as f64 + sum[1] / count) * s,
                    origin.z + (k as f64 + sum[2] / count) * s,
                ];

                let solid = (0..8).find(|&c| inside[c]).unwrap();
                let colour = palette(material.get(i + C[solid][0], j + C[solid][1], k + C[solid][2]));

                let eps = s * 0.5;
                let pv = Vec3::new(p[0], p[1], p[2]);
                let gx = (cv[1] + cv[2] + cv[5] + cv[6]) - (cv[0] + cv[3] + cv[4] + cv[7]);
                let gy = (cv[2] + cv[3] + cv[6] + cv[7]) - (cv[0] + cv[1] + cv[4] + cv[5]);
                let gz = (cv[4] + cv[5] + cv[6] + cv[7]) - (cv[0] + cv[1] + cv[2] + cv[3]);
                let len = (gx*gx + gy*gy + gz*gz).sqrt();
                let normal = if len > 1e-9 {
                    [(-gx/len) as f32, (-gy/len) as f32, (-gz/len) as f32]
                } else {
                    [0.0, 1.0, 0.0]
                };
                cell_vert[cidx(i, j, k)] = mesh.vertices.len() as u32;
                mesh.vertices.push(Vertex { pos: [p[0] as f32, p[1] as f32, p[2] as f32], normal, colour });
            }
        }
    }

    let mut emit = |q: [u32; 4], flip: bool| {
        if q.contains(&u32::MAX) { return; }
        let [a, b, c, d] = q;
        if flip {
            mesh.triangles.push([a, d, c]);
            mesh.triangles.push([a, c, b]);
        } else {
            mesh.triangles.push([a, b, c]);
            mesh.triangles.push([a, c, d]);
        }
    };

    for k in 1..nz - 1 {
        for j in 1..ny - 1 {
            for i in 1..nx - 1 {
                let here = val(i, j, k) >= iso;
                if here != (val(i + 1, j, k) >= iso) {
                    emit([cell_vert[cidx(i, j-1, k-1)], cell_vert[cidx(i, j, k-1)],
                             cell_vert[cidx(i, j, k)],     cell_vert[cidx(i, j-1, k)]], !here);
                }
                if here != (val(i, j + 1, k) >= iso) {
                    emit([cell_vert[cidx(i-1, j, k-1)], cell_vert[cidx(i, j, k-1)],
                             cell_vert[cidx(i, j, k)],     cell_vert[cidx(i-1, j, k)]], here);
                }
                if here != (val(i, j, k + 1) >= iso) {
                    emit([cell_vert[cidx(i-1, j-1, k)], cell_vert[cidx(i, j-1, k)],
                             cell_vert[cidx(i, j, k)],     cell_vert[cidx(i-1, j, k)]], !here);
                }
            }
        }
    }
    mesh
}

pub fn write_ply(mesh: &Mesh, path: &str) -> std::io::Result<()> {
    use std::io::Write;
    let mut f = std::io::BufWriter::new(std::fs::File::create(path)?);
    writeln!(f, "ply\nformat ascii 1.0")?;
    writeln!(f, "element vertex {}", mesh.vertices.len())?;
    writeln!(f, "property float x\nproperty float y\nproperty float z")?;
    writeln!(f, "property float nx\nproperty float ny\nproperty float nz")?;   // ← add
    writeln!(f, "property uchar red\nproperty uchar green\nproperty uchar blue")?;
    writeln!(f, "element face {}", mesh.triangles.len())?;
    writeln!(f, "property list uchar int vertex_indices")?;
    writeln!(f, "end_header")?;
    for v in &mesh.vertices {
        let ([x, y, z], [nx, ny, nz], [r, g, b]) = (v.pos, v.normal, v.colour);
        writeln!(f, "{x} {y} {z} {nx} {ny} {nz} {r} {g} {b}")?;                 // ← the missing line
    }
    for [a, b, c] in &mesh.triangles {
        writeln!(f, "3 {a} {b} {c}")?;
    }
    Ok(())
}