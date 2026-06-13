use crate::{Field, Vec3};

#[derive(Clone, Copy, PartialEq, Hash, Debug)]
pub struct FieldId(u32);

impl FieldId {
    fn index(self) -> usize {
        self.0 as usize
    }
}

#[derive(Default)]
pub struct Environment {
    fields: Vec<Box<dyn Field>>,
}

impl Environment {
    pub fn new() -> Self {
        Self::default()
    }

    pub fn add<F: Field + 'static>(&mut self, field: F) -> FieldId {
        let id = FieldId(self.fields.len() as u32);
        self.fields.push(Box::new(field));
        id
    }

    pub fn sample(&self, id: FieldId, p: Vec3) -> f64 {
        self.fields[id.index()].sample(p)
    }
}

#[derive(Default, Clone, Copy)]
pub struct EnvironmentSample {
    pub slope: f64,
    pub elevation: f64,
    pub temperature: f64,
    pub moisture: f64,
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::Constant;

    #[test]
    fn registers_and_samples() {
        let mut env = Environment::new();
        let a = env.add(Constant(1.0));
        let b = env.add(Constant(2.0));
        let origin = Vec3::new(0.0, 0.0, 0.0);
        assert_eq!(env.sample(a, origin), 1.0);
        assert_eq!(env.sample(b, origin), 2.0);
        assert_ne!(a, b);
    }
}