package com.e33epus.sweatyfeet;

/**
 * 玩家持久化汗脚数据（下线重进汗脚/真菌还在）。
 * Fabric 无 attachment 系统 → PlayerEntityMixin 实现本接口，数据存玩家 NBT：
 * - sweatState：汗脚当前等级（amp，-1=无）——脱鞋降级到底/洗脚清除时归 -1
 * - fungus：是否感染真菌
 * 上线的恢复逻辑在 SweatyFeetHandler（有状态但没效果 → 重新挂）。
 */
public interface SweatyDataHolder {
    int sweatState();

    void setSweatState(int amp);

    boolean hasFungus();

    void setFungus(boolean fungus);
}
