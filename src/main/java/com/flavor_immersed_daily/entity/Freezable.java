package com.flavor_immersed_daily.entity;

// 可被冻结的生物接口。
// 由 LivingEntityMixin 为所有 LivingEntity 实现：冻结期间用 isImmobile 禁止移动，
// 冻结计时只在服务端推进（AI 本身也只跑在服务端）。
public interface Freezable {

// 施加冻结刻数（取较长的一次，避免连续命中时缩短）
    void fid$freeze(int ticks);

// 当前是否处于冻结状态
    boolean fid$isFrozen();
}
