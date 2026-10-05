package com.qianyunyi.rider.util.book;

import com.qianyunyi.rider.util.StoryBookUtil;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;

public class GalandaFanaticDiaryBook {
    public static ItemStack create() {
        ListTag pages = new ListTag();

        // 第1页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"王朝历第17天\\n\",\"bold\":true,\"color\":\"dark_red\"}," +
                        "{\"text\":\"十面鬼哥尔哥斯就是个无能的废物。\\n奇顿那群蠢货守着古代印加的力量，却只会做些小打小闹的偷袭，连几个反抗的村落都清理不干净，根本不配复兴巴鲁齐亚王朝。\\n只有杰洛大帝，才是印加血脉的正统继承者，才配带领我们建立真正的帝国。\"}]"
        ));

        // 第2页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"今天完成了第3批兽人改造，用的是从奇顿带出来的印加秘术，改造完成度100%，力量、速度、抗性，全方面碾压奇顿的残次品。\\n那些被抓来的素材，全程嘶吼、挣扎，骂我们是恶魔。\\n真是可笑。\"}]"
        ));

        // 第3页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"能被改造成帝国的兽人战士，能为巴鲁齐亚王朝的复兴出力，是这些低贱凡人这辈子唯一的价值。\\n反抗？不过是徒劳的哀嚎罢了。\"}]"
        ));

        // 第4页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"王朝历第23天\\n\",\"bold\":true,\"color\":\"dark_red\"}," +
                        "{\"text\":\"清理了3个私藏奇顿残党的村落，抓了42个合格的改造素材。\\n其中有几个，是以前奇顿的老同事，他们骂我是叛徒，说我背叛了奇顿。\"}]"
        ));

        // 第5页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"叛徒？\\n我只是选择了真正的强者，选择了能带领我们走向荣耀的帝王。\\n奇顿已经烂透了，十面鬼的统治早就该结束了。加兰达，才是古代印加力量真正的继承者，才是这个世界未来的主人。\"}]"
        ));

        // 第6页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"有个新兵在改造时手软了，对着一个哭着求饶的孩子下不去手。\\n我当场就把他扔进了改造池，让他和那些素材一起，变成帝国的养料。\\n加兰达不需要软弱的废物。\\n对大帝的不忠，对王朝的不敬，就是死罪。\"}]"
        ));

        return StoryBookUtil.createWrittenBook(
                "加兰达狂热者日记",
                "█████(字迹难以辨认)",
                "§4一本沾满血污的笔记",
                pages
        );
    }
}