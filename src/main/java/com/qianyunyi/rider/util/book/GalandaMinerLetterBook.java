package com.qianyunyi.rider.util.book;

import com.qianyunyi.rider.util.StoryBookUtil;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;

public class GalandaMinerLetterBook {
    public static ItemStack create() {
        ListTag pages = new ListTag();

        // 第1页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"我叫朴正浩，是红土村的矿工。\\n写下这些话的时候，我正躲在火山的岩缝里，后面加兰达的追兵，应该很快就会找到我了。\\n我不怕死。我只是想完成我和哲秀的约定。\"}]"
        ));

        // 第2页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"我和哲秀是一个矿上的兄弟。我们一起下矿，一起喝酒，一起约定，等攒够了钱，就一起离开恶地，去平原的海边聚落开一家小酒馆，再也不用下矿挖红石，再也不用看矿主的脸色。\"}]"
        ));

        // 第3页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"结果还没等我们攒够钱，加兰达的人就来了。他们炸了矿洞，抓走了所有活着的矿工，说我们是“天生的印加秘术改造素材”，能为杰洛大帝效力，是我们的荣幸。\"}]"
        ));

        // 第4页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"我们被扔进了印加改造工坊。\\n他们用古代印加帝国的秘术，给我们注射了改造药剂，把我们的身体改造成了不怕火、不怕毒、不会累的不死劳工，逼我们在火山深处挖取古代印加遗迹的核心水晶，帮他们炼制兽人战士、激活征服世界的秘术。\"}]"
        ));

        // 第5页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"我和哲秀约定，一定要一起逃出去。不管付出什么代价，都要一起逃出去，去海边开我们的小酒馆。\"}]"
        ));

        // 第6页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"三个月前，我们终于等到了机会。帝国要在火山祭坛举行印加秘术仪式，所有的守卫都去了祭坛，工坊的防守很薄弱。我们偷了工坊里炼制好的印加爆破符文石，炸了工坊的围墙，带着十几个兄弟一起逃了出来。\"}]"
        ));

        // 第7页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"结果刚跑出没多远，帝国的追兵就来了。哲秀为了掩护我们，拿着爆破符文石冲回了追兵里，和他们同归于尽了。\\n他最后喊的话是：正浩，活下去！去海边！开我们的酒馆！\"}]"
        ));

        // 第8页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"只有我一个人活了下来。\\n我逃到了这个火山岩缝里，身上的伤越来越重，印加秘术改造的身体也开始崩溃了。我知道我逃不掉了，我也活不了多久了。\"}]"
        ));

        // 第9页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"我终于明白，加兰达帝国所谓的“重建巴鲁齐亚王朝”，所谓的“杰洛大帝的伟业”，全都是骗人的。\\n我们这些矿工，这些改造劳工，这些在前线打仗的兽人，在那个藏在祭坛最深处的大帝眼里，都只是挖取水晶的工具。用完了，废了，就没了。\"}]"
        ));

        // 第10页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"哲秀死了。那些和我们一起逃出来的兄弟也死了。我们的约定，再也完不成了。\\n如果有人捡到这张纸，如果你能去到平原的海边聚落，求你，在码头边开一家小酒馆，名字叫「哲秀和正浩的家」。\\n就当是，我们完成了约定。\"}]"
        ));

        return StoryBookUtil.createWrittenBook(
                "朴正浩的绝笔信",
                "朴正浩",
                "§6一名矿工的绝笔",
                pages
        );
    }
}