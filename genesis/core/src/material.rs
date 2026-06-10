#[derive(Clone, Copy, PartialEq, Eq, Hash, Debug, Default)]
pub struct MaterialId(pub u16);

impl MaterialId {
    pub const NONE: MaterialId = MaterialId(0);
    pub const fn index(self) -> usize {
        self.0 as usize
    }
}