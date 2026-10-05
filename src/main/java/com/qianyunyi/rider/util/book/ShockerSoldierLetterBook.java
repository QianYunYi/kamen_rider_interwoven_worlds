package com.qianyunyi.rider.util.book;

import com.qianyunyi.rider.util.StoryBookUtil;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;

public class ShockerSoldierLetterBook {
    public static ItemStack create() {
        ListTag pages = new ListTag();

        // 第1页 - 字迹潦草，有涂改
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"我头很痛。\\n每次想到过去的事，头就痛得要裂开。\\n韦贝尔博士说这是正常的，新植入的意识正在覆盖旧记忆。\\n可我还是想写点什么……趁我还能想。\"}]"
        ));

        // 第2页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"今天训练时，我听见远处的爆炸声。\\n是骑士，他们又来了。\\n我的手在发抖，不是因为害怕……是兴奋。为修卡而战，是荣誉。\\n可为什么我的胃在抽搐？\"}]"
        ));

        // 第3页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"昨晚又梦见那个女人了。\\n她在哭，喊一个名字……好像是我的名字，又好像不是。\\n醒来时，小队长站在我床边，他说我半夜在哭。\\n战斗员不该有眼泪。韦贝尔博士给我做了电击治疗。\\n现在我只记得要为修卡而战。\"}]"
        ));

        // 第4页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"今天看到新兵，是前天从村子带来的农民。\\n三分钟，就三分钟。\\n博士给他戴上头盔，通电，惨叫，然后他就站起来了。\\n眼神空洞，但行军礼很标准。\\n就像我当初一样。\"}]"
        ));

        // 第5页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"我偷偷藏了纸笔。\\n写这个很危险，被发现了要进惩戒室。\\n但我必须写……趁我还记得。\\n韦贝尔博士的机器不是完美的，有时候旧记忆会漏出来。\\n就像水管裂缝，滴答，滴答。\"}]"
        ));

        // 第6页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"佐尔上校今天视察。\\n他走过队列时，我闻到了血和铁锈的味道。\\n不是这个世界的味道，是更深的、更旧的……\\n我突然想起来，我见过雪，红色的雪。\\n然后头就痛得跪下了。\"}]"
        ));

        // 第7页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"电击治疗越来越频繁了。\\n博士说我的‘残留抗性’很高。\\n什么是抗性？我只是想记起我是谁。\\n不，不对……我是修卡战斗员，编号743。\\n这就是我。必须是我。\"}]"
        ));

        // 第8页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"今天执行任务，烧村子。\\n有个老人不肯走，跪着求我们。\\n他说他儿子两年前被抓走了，问我见过没有。\\n我看着他的眼睛，突然喘不上气。\\n小队长开枪了。任务完成。\"}]"
        ));

        // 第9页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"我可能快不行了。\\n每次电击后，忘记的就更多。\\n昨天我对着镜子看了很久，那张脸很陌生。\\n博士说这是进化，是成为完美士兵的代价。\\n可为什么我的心在痛？\"}]"
        ));

        // 第10页 - 字迹开始混乱
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"如果你找到这封信……我不知道你是谁。\\n但如果你有家人，有记忆，有名字……\\n跑。\\n别被他们抓到。\\n别变成我这样。\\n我甚至不记得我在警告谁。\\n\\n编号743……还是……我本来叫什么？\"}]"
        ));

        // 第11页 - 最后几行几乎无法辨认
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"头又要痛了。\\n我听到脚步声，是博士的人。\\n他们要来给我‘治疗’了。\\n这大概是最后一次……\\n\\n修卡……万岁……\\n不……我不想……\\n\\n（以下字迹被水渍晕开，无法辨认）\"}]"
        ));

        return StoryBookUtil.createWrittenBook(
                "破碎的记录",
                "???(编号743)",
                "§8被涂改多次的残破纸张",
                pages
        );
    }
}