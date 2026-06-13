use crate::{EnvironmentSample, MaterialId};

pub trait MaterialSelector {
    fn select(&self, ctx: &EnvironmentSample, depth: f64) -> MaterialId;
}

pub struct Fixed(pub MaterialId);

impl MaterialSelector for Fixed {
    fn select(&self, _: &EnvironmentSample, _: f64) -> MaterialId { self.0 }
}

pub struct BySlope {
    pub gentle: MaterialId,
    pub steep: MaterialId,
    pub threshold: f64,
}

impl MaterialSelector for BySlope {
    fn select(&self, ctx: &EnvironmentSample, _: f64) -> MaterialId {
        if ctx.slope >= self.threshold { self.steep } else { self.gentle }
    }
}

pub struct Draped {
    pub over: MaterialId,
    pub under: MaterialId,
    pub max_depth: f64,
    pub gentle: f64,
    pub steep: f64,
}

impl MaterialSelector for Draped {
    fn select(&self, ctx: &EnvironmentSample, depth: f64) -> MaterialId {
        let t = ((ctx.slope - self.gentle) / (self.steep - self.gentle)).clamp(0.0, 1.0);
        let scree_depth = self.max_depth * t;
        if depth <= scree_depth { self.over } else { self.under }
    }
}