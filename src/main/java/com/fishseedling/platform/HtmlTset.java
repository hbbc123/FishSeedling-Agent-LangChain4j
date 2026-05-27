package com.fishseedling.platform;

public class HtmlTset {
    public static void main(String[] args) {
        String html= """
                <h2>一、品种特点</h2>
                <p>鲫鱼体侧扁而高，头小，吻钝。无须。体背部为灰黑色，腹部银白色。</p>
                <h2>二、养殖优势</h2>
                <ul>
                <li>适应性强：耐低温、耐低氧</li>
                <li>食性杂：荤素兼食</li>
                <li>生长快：工程鲫等优良品种生长迅速</li>
                <li>抗病力强：成活率高</li>
                </ul>
                <h2>三、养殖模式</h2>
                <h3>1. 主养模式</h3>
                <p>亩放养规格10-20g的鱼种1500-2000尾。</p>
                <h3>2. 混养模式</h3>
                <p>作为配养鱼，每亩放养50-100尾。</p>
                <h3>3. 网箱养殖</h3>
                <p>适合在水库、湖泊进行网箱养殖。</p>
                <h2>四、养殖技术</h2>
                <ul>
                <li>投喂：配合饲料为主</li>
                <li>水质：保持清新，定期换水</li>
                <li>防病：注意预防寄生虫病</li>
                </ul>
                
                """;
        // 去除 HTML 标签
        String text = html.replaceAll("<[^>]+>", "");

        // 将连续的空白字符（包括换行、空格、制表符）替换为空格
        text = text.replaceAll("\\s+", " ");

        // 将句号后的空格保留，但确保列表项之间用句号分隔
        // 先处理顿号和冒号
        text = text.replaceAll("([：、])", "。");

        // 确保每个标题后都有句号
        text = text.replaceAll("([一二三四五六七八九十]、)", "$1");

        // 将数字序号后的空格或内容处理
        text = text.replaceAll("(\\d+\\.\\s*)", "");

        // 清理多余空格
        text = text.trim();

        System.out.println(text);
    }
}
