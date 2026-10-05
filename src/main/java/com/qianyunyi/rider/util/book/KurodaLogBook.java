package com.qianyunyi.rider.util.book;

import com.qianyunyi.rider.util.StoryBookUtil;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;

public class KurodaLogBook {
    public static ItemStack create() {
        ListTag pages = new ListTag();

        // 第1页：第173天
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"第173天\\n今天清理了三个反抗的村庄，抓了27个合格的改造素材。其中有几个，是我以前当老师时教过的学生。\\n他们认出我了，哭着喊我黑田老师，求我放了他们。真可笑。以前的我，真是软弱到令人作呕。居然会浪费时间教这些废物读书写字，居然会觉得这些只会种地、只会哭的蝼蚁有活着的价值。\"}]"
        ));

        // 第2页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"感谢首领，感谢修卡。改造手术洗去的不是我的记忆，是我的枷锁。只有力量才是真理，只有进化才有资格活下去。这些软弱的凡人，唯一的价值，就是成为改造素材，为修卡的大业添一块砖。\"}]"
        ));

        // 第4页：第175天
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"第175天\\n有个新兵蛋子手软了，对着一个抱着孩子的女人下不去手。我当场就毙了他。修卡不需要软弱的废物。\\n对敌人的仁慈，就是对首领的背叛。明天要去清理针叶林里的聚落，我已经安排好了埋伏。我会让他们知道，反抗修卡的下场，只有死。或者，成为我们的一部分。\"}]"
        ));

        return StoryBookUtil.createWrittenBook(
                "修卡小队长·作战日志",
                "黑田",
                "§4修卡小队长·作战日志",
                pages
        );
    }
}