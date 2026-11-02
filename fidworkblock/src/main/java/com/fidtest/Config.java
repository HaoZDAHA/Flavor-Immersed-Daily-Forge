package com.fidtest;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

// 开局是否发放教程卷轴
    public static final ForgeConfigSpec.BooleanValue GIVE_STARTBOOK_ON_JOIN = BUILDER
            .comment("玩家首次进入存档时是否发放教程卷轴，默认 true；关闭后不再发放")
            .define("giveStartbookOnJoin", true);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    private Config() {}
}
