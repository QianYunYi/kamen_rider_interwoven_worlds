package com.qianyunyi.rider.util.book;

import com.qianyunyi.rider.util.StoryBookUtil;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;

public class YamayokeIronbloodBook {
    public static ItemStack create() {
        ListTag pages = new ListTag();

        // 第1页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"我叫山冈铁平，是北鹿村的猎户。\\n写下这些字的时候，我的右手已经开始怪人化了。指甲变成了毒钳，胳膊上长满了硬壳，再过不久，我就会变成那些没有脑子的怪物，只会听那些穿黑衣服的人的话，只会杀人。\\n我不想变成怪物。我想回家。\"}]"
        ));

        // 第2页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"三个月前，盖尔修卡的人闯进了我们村子。他们开着装甲车，拿着枪，抓走了村里所有的青壮年。他们说，我们是“神选的改造素材”，能为伟大的首领效力，是我们的荣幸。\\n我被扔进了改造舱。冰冷的液体灌满了我的口鼻，骨头像被生生敲碎再重新拼起来一样疼。\"}]"
        ));

        // 第3页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"我看着身边的人一个个变成了蝎子怪物，一个个失去了神智，只会嘶吼。\\n我运气好，改造失败了。他们说我是残次品，把我扔进了这个地牢，等着被当成饲料喂给那些完全改造的怪物。\\n我靠着地牢里的老鼠和渗水，活了下来。\"}]"
        ));

        // 第4页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"我每天都在墙上刻我老婆和女儿的名字。我答应过女儿，等秋天到了，就带她去山里打兔子，给她做兔子皮的手套。我答应过老婆，等今年的猎物卖了钱，就给她换一副新的银镯子。\\n我不能死在这里。我不能变成怪物。\"}]"
        ));

        // 第5页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"我试过用石头砸断自己改造的胳膊，没用。硬壳比石头还硬。我试过咬舌自尽，也没用。他们给我改造的身体，连死都做不到主。\\n那些穿黑衣服的人，每天都会来地牢门口看。他们笑着说，这些残次品的存活数据，会给首领的计划提供很好的参考。\"}]"
        ));

        // 第6页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"原来我们这些人，在他们眼里，连人都不算。只是一组数据，一个素材，一个用完就丢的垃圾。\\n如果有人捡到这张血书，如果你能去到北鹿村，求你帮我带一句话给我的老婆和女儿。\\n就说爸爸对不起她们，爸爸没能遵守约定。\\n就说爸爸不是怪物。\"}]"
        ));

        return StoryBookUtil.createWrittenBook(
                "山冈铁平的血书",
                "山冈铁平",
                "§4一封写在布片上的血书",
                pages
        );
    }
}