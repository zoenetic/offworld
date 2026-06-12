#[derive(Clone, Debug)]
pub struct Material {
    pub name: String,
    pub hardness: f64,
    pub colour: [u8; 3],
}

#[derive(Default)]
pub struct MaterialCatalogue {
    materials: Vec<Material>,
}

impl MaterialCatalogue {
    pub fn new() -> Self {
        Self::default()
    }

    pub fn add(&mut self, material: Material) -> MaterialId {
        let id = MaterialId((self.materials.len() + 1) as u16);
        self.materials.push(material);
        id
    }

    pub fn get(&self, id: MaterialId) -> Option<&Material> {
        if id == MaterialId::NONE {
            return None;
        }
        self.materials.get(id.index() - 1)
    }
}

#[derive(Clone, Copy, PartialEq, Eq, Hash, Debug, Default)]
pub struct MaterialId(pub u16);

impl MaterialId {
    pub const NONE: MaterialId = MaterialId(0);
    pub const fn index(self) -> usize {
        self.0 as usize
    }
}