package com.qianyunyi.rider.util.book;

import com.qianyunyi.rider.util.StoryBookUtil;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;

public class DelzarCommanderDiaryBook {
    public static ItemStack create() {
        ListTag pages = new ListTag();

        // 第1页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"我是伊万，迪尔扎雪原战线总指挥官。\\n今天，我彻底扫平了反抗的雪原聚落。\\n300多个反抗者，全部被我冻成了冰雕，就摆在堡垒的门口，当装饰品。\\n真是可笑。\"}]"
        ));

        // 第2页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"他们居然还喊着我以前的名字，求我放过他们，说我以前是守护雪原的英雄。\\n英雄？\\n那是我这辈子听过最可笑的词。\"}]"
        ));

        // 第3页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"以前的我，居然会为了保护这些软弱的废物，和迪尔扎的军团作对。\\n结果呢？我的聚落被踏平，我的妻子和孩子，被抓进了改造所。\\n是迪尔扎的首领，给了我新生。\"}]"
        ));

        // 第4页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"他让我明白了，这个世界的真理，从来都不是什么守护，而是弱肉强食。\\n强者生，弱者死。就这么简单。\"}]"
        ));

        // 第5页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"我把我的妻子，改造成了冰封战狼。\\n我把我的孩子，改造成了冰冻斥候。\\n他们现在，拥有了永恒的生命，拥有了绝对的力量，再也不会被软弱的血肉束缚。\"}]"
        ));

        // 第6页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"他们应该感谢我。\\n那些被我冻成冰雕的废物，也应该感谢我。\\n我让他们以另一种形式，永远留在了这片雪原上，成为了强者的装饰。\"}]"
        ));

        // 第7页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"明天，我会带着我的军团，扫平最后一个反抗的冻土聚落。\\n所有反抗迪尔扎的人，所有软弱的废物，都将被冰封，都将成为我荣耀的垫脚石。\\n为了迪尔扎，为了首领，为了雪原的绝对秩序。\"}]"
        ));

        return StoryBookUtil.createWrittenBook(
                "伊万的日记",
                "伊万",
                "§b迪尔扎雪原战线总指挥官的手记",
                pages
        );
    }
}