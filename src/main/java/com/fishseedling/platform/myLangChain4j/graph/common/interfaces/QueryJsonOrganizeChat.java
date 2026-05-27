package com.fishseedling.platform.myLangChain4j.graph.common.interfaces;

import dev.langchain4j.service.*;

public interface QueryJsonOrganizeChat {
   @SystemMessage("""
           请将以下数据格式化为HTML标签 结构展示给用户只需要HTML不要返回makedown格式不要返回其他内容：
           -其中data-type的值如果有{id}字符替换成列表数据中的id请根据数据生成
            
           
           数据内容为:
            {{json}}
           情况一:如果json的data不为空
                   -记住数据内容为列表或单条数据都可以生成如下格式的 HTML
                   
                    -生成如下格式的 HTML：
                               
                    <div class="fish-supply-list">
                      {循环生成}
                      <div class="card" data-id="列表数据中的id请根据数据生成" data-type="{{type}}">
                        <div class="card-header">
                          <h3>草鱼</h3>
                          <h3>供应</h3>
                        </div>
                        <div class="card-body">
                          <p>📍 湖南省 长沙市 望城区 望城水产养殖基地</p>
                          <p>规格:5-8cm</p>
                          <p>数量:50万尾</p>
                          <p>💰 0.15/元/尾</p>
                          <p>优质草鱼苗，生长快，成活率高达95%，支持现场看苗，可提供运输服务。</p>
                        </div>
                        <div class="card-footer">
                          <button class="btn-view" data-id="列表数据中的id请根据数据生成">查看详情</button>
                        </div>
                      </div>
                      {结束循环}
                    </div>
            情况二:如果code不等于200或者data为空或者msg也返回一个被<div class="api-data-empty"></div >包裹的提示信息
                -根据内容友好的向用户输出原因要优雅可以添加标签
                
                
           重要注意:
            -不要返回```这种makedown格式
            -只用返回纯文本，不要加引号，上引号
           """)
     TokenStream chat(@V("json") String json, @V("type") String type,@UserMessage String msg);
}
