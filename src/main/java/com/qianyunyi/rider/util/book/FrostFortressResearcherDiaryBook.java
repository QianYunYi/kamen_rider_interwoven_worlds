package com.qianyunyi.rider.util.book;

import com.qianyunyi.rider.util.StoryBookUtil;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;

public class FrostFortressResearcherDiaryBook {
    public static ItemStack create() {
        ListTag pages = new ListTag();

        // 第1页 - 研究员的日记
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"第589天 暴雪\\n我被迫来到了这个冰封堡垒。\\n迪尔扎的人闯进了我的雪原研究所，杀了我的助手，烧毁了我的研究资料。他们说，只要我帮他们改造冰封生物，就放过我远在冻土聚落的父母。\\n我没得选。\"}]"
        ));

        // 第2页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"我是研究极地生物的，我这辈子都在和雪原的动物打交道，我了解它们，我爱它们。\\n现在，他们逼我亲手把这些美丽的生灵，改造成没有感情的杀人机器。\"}]"
        ));

        // 第3页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"第612天 阴\\n他们给我送来了一只北极狼幼崽。\\n它的妈妈被迪尔扎的巡逻队杀了，它的腿受了伤，缩在笼子里，浑身发抖，却还是对着我龇牙，眼里全是倔强。\\n我给它处理了伤口，喂了它肉干。\"}]"
        ));

        // 第4页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"它慢慢放下了戒备，会用脑袋蹭我的手，会趴在我的脚边睡觉。\\n我给它取名叫白雪。\\n迪尔扎的军官说，这只幼崽的天赋极好，是改造的完美素材，让我尽快给它注射改造药剂。\"}]"
        ));

        // 第5页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"我拖着，一直拖着。\\n我不想让白雪变成那些只会杀人的怪物。它应该在雪原的雪地里奔跑，而不是被关在笼子里，变成杀人的工具。\"}]"
        ));

        // 第6页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"第647天 晴\\n白雪长大了。\\n它的腿伤完全好了，长得高大又漂亮，会跟着我在堡垒的走廊里散步，会帮我叼来实验用的记录本，会在我难过的时候，用脑袋蹭我的脸。\"}]"
        ));

        // 第7页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"它是我在这个冰冷的地狱里，唯一的光。\\n但我知道，我拖不了多久了。军官已经给我下了最后通牒，下个月之前，必须完成白雪的改造，否则就杀了我的父母，杀了我，杀了白雪。\\n我偷偷做了一个决定。\"}]"
        ));

        // 第8页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"第672天 绝笔\\n我决定带着白雪逃出去。\\n我准备了爆破水晶，计划今晚行动。\\n如果成功，我们就能重获自由。\\n如果失败……不，不会失败。\\n我们一定要一起逃出去。\\n\"},{\"text\":\"（日记在这里中断了）\",\"italic\":true,\"color\":\"gray\"}]"
        ));

        return StoryBookUtil.createWrittenBook(
                "冰封堡垒的研究日记",
                "???(笔迹被烧毁)",
                "§b一本残破的皮质笔记本",
                pages
        );
    }
}