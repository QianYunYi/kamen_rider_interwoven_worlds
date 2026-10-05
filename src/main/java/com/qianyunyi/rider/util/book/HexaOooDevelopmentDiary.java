package com.qianyunyi.rider.util.book;

import com.qianyunyi.rider.util.StoryBookUtil;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;

public class HexaOooDevelopmentDiary {
    public static ItemStack create() {
        ListTag pages = new ListTag();

        // 第1页：3月12日 项目启动
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"3月12日\\n项目启动。编号HEXA-OOO-001。匿名发件人传来了完整的OOO驱动器战斗数据、核心硬币构造解析图，精度高到离谱，甚至包含了驱动器内部的能量流动逻辑，比我们从鸿上基金会窃取到的资料完整10倍。\"}]"
        ));

        // 第2页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"指令要求：以OOO系统为蓝本，150天内完成一套六插槽驱动器的开发。发件人没有署名，只有一个修卡的标志。我们都知道，这是首领的指令。\\n没人敢问为什么要做，没人敢问这套系统是给谁用的。在修卡，首领的指令就是唯一的规则。\"}]"
        ));

        // 第3页：5月19日 核心硬币复刻完成
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"5月19日\\n核心硬币复刻完成。首领给的六块结晶样本，我们甚至不需要分析成分，只需要按照他给的参数，复刻成适配驱动器的硬币即可。他给的参数精准到了原子级别，没有任何试错的空间，也没有任何修改的余地。\\n六枚硬币，对应六个已经覆灭的组织：修卡、盖尔修卡、迪斯特龙、GOD机关、加兰达、迪尔扎。\"}]"
        ));

        // 第4页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"我突然觉得，从几十年前修卡创立开始，这六枚硬币的准备工作，就已经启动了。首领好像早就知道，几十年后，会有OOO这样一套系统出现。他好像能看到未来。\"}]"
        ));

        // 第5页：8月6日 项目完成
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"8月6日\\n项目完成。耗时147天，比要求的工期提前了3天。六连驱动器调试完成，六枚硬币装填、扫描、变身流程完全顺畅，系统定名：修卡首领-OOOOOO-。\\n测试数据显示，这套系统的力量上限，远超OOO的任何已知联组，甚至具备直接粉碎核心硬币的能力。\"}]"
        ));

        // 第6页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"我给首领发了最终的测试报告，半小时后，收到了他的回复，只有两个字：收到。没有表扬，没有指令，没有后续的安排。\\n就好像，他只是完成了一场游戏里的一个小任务。我到现在，都没见过首领的面，没听过他的声音，甚至不知道他是不是在这个星球上。\"}]"
        ));

        // 第7页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"我只知道，我们做出来的，不是一套驱动器。是一个新的棋局的开局。\"}]"
        ));

        // 第8页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"（后续记录已被强制清除）\"}]"
        ));

        return StoryBookUtil.createWrittenBook(
                "HEXA-OOO-001开发日记",
                "佚名研究员",
                "§4HEXA-OOO-001开发日记",
                pages
        );
    }
}