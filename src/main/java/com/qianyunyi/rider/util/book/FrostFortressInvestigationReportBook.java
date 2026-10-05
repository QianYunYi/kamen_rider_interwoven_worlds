package com.qianyunyi.rider.util.book;


import com.qianyunyi.rider.util.StoryBookUtil;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;

public class FrostFortressInvestigationReportBook {
    public static ItemStack create() {
        ListTag pages = new ListTag();

        // 第1页 - 调查报告
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"【迪尔扎军团事件调查报告】\\n\\n地点：冰封堡垒B3区\\n时间：████年█月█日\\n调查员：█████\\n\\n在堡垒出口附近发现剧烈战斗痕迹，现场有爆炸残留物和多具迪尔扎士兵遗体。\"}]"
        ));

        // 第2页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"现场发现一具成年北极狼遗骸，体型比正常个体大30%。毛色纯白，无明显改造痕迹。尸体呈保护姿态，挡在一具人类遗骸前方。\\n人类遗骸穿着研究员制服，手中紧握爆破水晶引爆装置。经鉴定，为堡垒生物改造部首席研究员████。\"}]"
        ));

        // 第3页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"结合现场痕迹还原：\\n研究员与北极狼（代号白雪）试图从B3区出口逃脱，被迪尔扎巡逻队拦截。研究员引爆了随身携带的爆破水晶，与追兵同归于尽。\\n值得注意的是，北极狼并未参与攻击，而是全程保护研究员。\"}]"
        ));

        // 第4页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"我们在研究员遗骸附近发现了这本日记（附件A），记录了他被迫为迪尔扎工作，以及与白雪相遇、相处的经过。\\n从日记看，白雪是唯一未接受改造的实验体，研究员宁可牺牲自己也不愿让白雪成为杀人工具。\"}]"
        ));

        // 第5页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"迪尔扎内部记录显示，研究员████的父母已于三个月前在冻土聚落'意外身亡'。显然，迪尔扎从未打算履行承诺。\\n研究员至死都不知道这个真相。\"}]"
        ));

        // 第6页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"结论：\\n1. 迪尔扎军团使用人质要挟手段强迫科学家合作\\n2. 白雪是自然北极狼，未接受任何改造\\n3. 研究员与白雪建立了深厚情感联结\\n4. 迪尔扎的承诺均为谎言，人质早已遇害\\n\\n附件：研究员日记副本（已归档）\"}]"
        ));

        // 第7页
        pages.add(StoryBookUtil.page(
                "[{\"text\":\"补充记录：\\n我们在现场还发现了一些不寻常的细节。那只北极狼的爪子和牙齿上没有任何血迹，说明它在整个冲突中从未攻击任何人。\\n它只是挡在主人面前，直到最后。\\n\\n有时候，野兽比人类更懂得何为忠诚。\"}]"
        ));

        return StoryBookUtil.createWrittenBook(
                "冰封堡垒事件调查报告",
                "█████(调查员)",
                "§3迪尔扎军团档案·绝密",
                pages
        );
    }
}