package com.qianyunyi.rider.util.book;

import com.qianyunyi.rider.util.StoryBookUtil;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;

public class ShockerResearcherDiaryBook {
    public static ItemStack create() {
        ListTag pages = new ListTag();

        // 第1页：8月3日 晴
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"8月3日 晴\\n今天终于入职了。人事部的人说，只要我好好干，满三个月就能拿到特殊津贴，还有优先使用总部医疗舱的权限。妹妹的肺病不能再拖了。乡下的医院说，最多再撑半年。只有修卡的医疗舱能救她。我被分到了3号培养基房，负责培养改造人用的活性病毒株。带我的前辈说，别问，别听，别多看，做好自己的事就行。我记住了。我只想救妹妹。\"}]"
        ));


        // 第2页：10月15日 雨
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"10月15日 雨\\n前辈失踪了。昨天他还在跟我说，这批病毒株的毒性太强了，根本不是用来做改造人强化的，是用来做无差别扩散的。他说他要去找上面的人反映，不能拿整个城市的人开玩笑。\\n今天他的工位就空了。储物柜被清空，所有的实验记录都被销毁了。我不敢问，不敢多说一个字。我把他偷偷留给我的病毒株备份，藏在了储物柜的最里面。\"}]"
        ));

        // 第3页：12月24日 阴
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"12月24日 阴\\n我拿到了医疗舱的使用权限。妹妹被接进了总部的附属医院，医生说，再过一个疗程，她的肺病就能痊愈了。\\n我太开心了。哪怕每天对着那些令人作呕的培养基，哪怕每天都要听着隔壁改造人牢房里的惨叫，我都觉得值了。只是有件事很奇怪。\\n我负责培养的这批病毒株，最终的投放地点，竟然包括了妹妹所在的医院所在的街区。\"}]"
        ));

        // 第4页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"我去问了项目组的人，他们只说我多管闲事，让我做好自己的事。\"}]"
        ));

        // 第5页：2月17日 绝笔
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"2月17日 绝笔\\n病毒泄露了。不是意外。是上面的人故意安排的。他们要测试这批病毒的实际效果，就拿整个总部附属区的人做了实验。\\n妹妹所在的医院，是第一个被感染的。我冲进医院的时候，她已经没气了。她的手里还攥着给我织了一半的围巾，上面沾着血。\"}]"
        ));

        // 第6页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"我终于明白前辈说的是什么了。我们这些人，我们的亲人，我们拼了命想要守护的东西，在上面的人眼里，都只是实验的耗材，是测试数据的样本。\\n我把中和试剂的配方写在了日记的最后一页。如果有人捡到这本日记，求你，一定要阻止他们。别再让更多人像我妹妹一样，白白死去。\"}]"
        ));

        // 第7页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"(最后一页被撕掉了)\"}]"
        ));

        return StoryBookUtil.createWrittenBook(
                "研究员日记",
                "无名氏",
                "§7染血的研究员日记",
                pages
        );
    }
}