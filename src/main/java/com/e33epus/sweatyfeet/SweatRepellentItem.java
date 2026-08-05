package com.e33epus.sweatyfeet;

import net.minecraft.world.item.Item;

/**
 * 花露水（Floral Water）：真菌治疗的"两步走"第一步。
 * 配方：水瓶 + 铃兰 + 萤石粉（工作台）。
 * 用法：对"装有清水"的洗脚盆右键 → 盆水变药水洗脚水（交互在 WashBasinBlock，
 * 花露水不再直接喝/喷治真菌——文档拍板：真菌必须用药水洗脚水泡脚才能治，直接喝无效）。
 * 梗：花露水其实治不了脚气，游戏里却要它来治真菌 = 整蛊反差。
 */
public class SweatRepellentItem extends Item {
    public SweatRepellentItem(Properties properties) {
        super(properties);
    }
}
