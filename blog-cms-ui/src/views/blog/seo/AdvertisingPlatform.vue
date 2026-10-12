<template>
	<el-card>
		<div slot="header" class="card-header">
			<div><span class="card-title">广告平台关联</span><span class="card-tip">当前支持 Google AdSense，保存后动态生效</span></div>
			<el-button type="primary" size="small" icon="el-icon-check" :loading="saving" @click="submit">保存配置</el-button>
		</div>
		<el-alert title="Google AdSense 广告配置" type="info" :closable="false" show-icon>
			<div slot="default">页面表单会自动组装为一份 AdSense JSON 保存。手动广告位只需粘贴 Google 提供的完整代码。</div>
		</el-alert>

		<el-form :model="form" label-width="130px" size="small" class="adsense-form">
			<div class="master-switch">
				<div><strong>启用 AdSense</strong><p>平台总开关；关闭后保留配置，但前台不加载或渲染任何广告。</p></div>
				<el-switch v-model="form.enabled" active-text="已启用" inactive-text="已关闭"/>
			</div>
			<section class="config-section">
				<SectionTitle index="1" title="发布商配置" desc="一个 AdSense 账号全站只使用一个发布商 ID。"/>
				<el-form-item label="发布商 ID"><el-input v-model.trim="form.client" placeholder="ca-pub-1234567890123456" clearable class="normal-input"/><div class="field-tip">来自 AdSense 代码中的 <code>client</code>；粘贴广告单元代码时也会自动识别。</div></el-form-item>
			</section>

			<section class="config-section">
				<SectionTitle index="2" title="自动广告" desc="用于「按网站」投放，广告位置由 Google 自动决定。"/>
				<el-form-item label="开启自动广告"><el-switch v-model="form.autoAds" :disabled="!form.enabled" active-text="开启" inactive-text="关闭"/><div class="field-tip">还需在 Google AdSense「Ads → By site」中开启该站点的自动广告。</div></el-form-item>
			</section>

			<section class="config-section">
				<div class="section-action">
					<SectionTitle index="3" title="ads.txt 配置" desc="用于声明站点授权 Google 销售广告库存。"/>
					<el-button type="primary" plain size="small" icon="el-icon-download" :loading="validatingAdsTxt" @click="verifyAdsTxt">验证并下载</el-button>
				</div>
				<el-form-item label="ads.txt 内容">
					<el-input v-model="form.adsTxt" type="textarea" :rows="3" resize="vertical" placeholder="可留空，系统将根据发布商 ID 自动生成" class="ads-textarea"/>
					<div class="field-tip">留空时自动生成 <code>google.com, pub-..., DIRECT, f08c47fec0942fa0</code>；填写后原样输出到 <code>/ads.txt</code>。</div>
				</el-form-item>
			</section>

			<section class="config-section">
				<div class="section-action">
					<SectionTitle index="4" title="广告位配置" desc="可按需新增多个广告位，直接粘贴 Google 提供的代码。"/>
					<el-button type="primary" plain size="small" icon="el-icon-plus" @click="addSlot">新增广告位</el-button>
				</div>
				<div v-if="form.slots.length" class="slot-list">
					<div v-for="(row,index) in form.slots" :key="index" class="slot-card">
						<div class="slot-card-head"><strong>广告位 {{ index + 1 }}</strong><div><el-switch v-model="row.enabled" active-text="启用"/><el-button type="text" class="danger-text" @click="removeSlot(index)">删除</el-button></div></div>
						<el-form-item label="投放位置">
							<el-select v-model="row.position" placeholder="请选择投放位置" class="position-select">
								<el-option v-for="opt in positionOptions" :key="opt.value" :label="opt.label" :value="opt.value"/>
							</el-select>
							<div class="field-tip">选择该广告位在页面中的投放位置；右栏位建议配合纵向广告样式。</div>
						</el-form-item>
						<el-form-item label="Google 代码"><el-input v-model="row.codeInput" type="textarea" :rows="8" resize="vertical" placeholder="粘贴 Google AdSense 提供的完整广告单元代码" @blur="parseSlotCode(row,true)"/><div class="field-tip">系统会自动识别 client、slot、format、layout 等参数，不会原样执行粘贴的脚本。</div></el-form-item>
						<div v-if="row.slot" class="parse-result">已识别：<code>slot={{ row.slot }}</code><code>format={{ row.format || 'auto' }}</code><code v-if="row.layout">layout={{ row.layout }}</code><code v-if="row.layoutKey">layout-key={{ row.layoutKey }}</code></div>
					</div>
				</div>
				<div v-else class="empty-slots">暂无手动广告位，可仅依靠 Google 自动广告，或点击“新增广告位”添加。</div>
				<div class="field-tip slots-tip">新增广告位会按顺序自动分配一个默认位置，可在广告位内改为其它投放位置。</div>
			</section>
		</el-form>
	</el-card>
</template>

<script>
	import {getAdsenseConfig, updateAdsenseConfig, downloadSeoFile, getAdsensePositions} from '@/api/seo'

	const SectionTitle = {functional:true, props:['index','title','desc'], render(h,ctx){return h('div',{class:'section-heading'},[h('span',{class:'section-index'},ctx.props.index),h('div',[h('h3',ctx.props.title),h('p',ctx.props.desc)])])}}
	const emptySlot = () => ({name:'',slot:'',position:'',enabled:true,format:'auto',layout:'',layoutKey:'',fullWidth:null,alignment:'center',codeInput:''})
	const buildSlotCode = (slot,client) => slot.slot ? `<ins class="adsbygoogle"\n     style="display:block; text-align:${slot.alignment||'center'};"\n     data-ad-layout="${slot.layout||''}"${slot.layoutKey?`\n     data-ad-layout-key="${slot.layoutKey}"`:''}\n     data-ad-format="${slot.format||'auto'}"\n     data-ad-client="${client||''}"\n     data-ad-slot="${slot.slot}"${slot.fullWidth==null?'':`\n     data-full-width-responsive="${slot.fullWidth}"`}></ins>\n<script>\n     (adsbygoogle = window.adsbygoogle || []).push({});\n<\/script>` : ''

	export default {
		name:'AdvertisingPlatform', components:{SectionTitle},
		data(){return{saving:false,validatingAdsTxt:false,form:{version:1,enabled:false,autoAds:false,client:'',adsTxt:'',slots:[]},internalPositions:['articleTop','articleBottom','articleInArticle','articleSidebarToc','listTop'],positionOptions:[{value:'articleTop',label:'文章详情·标题下方'},{value:'articleBottom',label:'文章详情·正文结束后'},{value:'articleInArticle',label:'文章详情·正文内嵌'},{value:'articleSidebarToc',label:'文章详情·右侧目录下方'},{value:'listTop',label:'列表页·顶部'}]}},
	created(){this.getData();this.loadPositions()},
	methods:{
		loadPositions(){getAdsensePositions().then(res=>{if(res&&res.data&&res.data.length)this.positionOptions=res.data}).catch(()=>{})},
		getData(){getAdsenseConfig().then(res=>{const d=res.data||{};this.form={version:d.version||1,enabled:d.enabled===true,autoAds:d.autoAds===true,client:d.client||'',adsTxt:d.adsTxt||'',slots:(d.slots||[]).map(i=>{const row=Object.assign(emptySlot(),i);row.codeInput=buildSlotCode(row,d.client);return row})}})},
		addSlot(){const p=this.internalPositions.find(p=>!this.form.slots.some(s=>s.position===p));if(!p){this.$message.warning('已达到当前支持的广告位数量上限');return}const s=emptySlot();s.position=p;this.form.slots.push(s)},
		removeSlot(i){this.form.slots.splice(i,1)},
		verifyAdsTxt(){
			this.validatingAdsTxt=true
			downloadSeoFile('ads').then(res=>{
				const blob=res.data instanceof Blob?res.data:new Blob([res.data],{type:'text/plain;charset=UTF-8'})
				if(!blob.size){this.$message.error('ads.txt 文件为空，请先配置发布商 ID 或 ads.txt 内容');return}
				const url=window.URL.createObjectURL(blob);const link=document.createElement('a');link.href=url;link.download='ads.txt';document.body.appendChild(link);link.click();document.body.removeChild(link);window.URL.revokeObjectURL(url);this.$message.success('ads.txt 验证成功，已开始下载')
			}).finally(()=>{this.validatingAdsTxt=false})
		},
		parseSlotCode(row,notify=false){
			if(!row.codeInput){row.slot='';return row.enabled?'请粘贴 Google 广告单元代码':''}
			const doc=new DOMParser().parseFromString(row.codeInput,'text/html');const ins=doc.querySelector('ins.adsbygoogle');
			if(!ins){const msg='未识别到 <ins class="adsbygoogle"> 广告单元';if(notify)this.$message.error(msg);return msg}
			const script=doc.querySelector('script[src*="adsbygoogle.js"]');let client=ins.getAttribute('data-ad-client')||'';
			if(!client&&script){try{client=new URL(script.getAttribute('src'),location.origin).searchParams.get('client')||''}catch(e){client=''}}
			const slot=ins.getAttribute('data-ad-slot')||'';if(!/^ca-pub-\d+$/.test(client)||!/^\d+$/.test(slot)){const msg='代码中的 client 或 slot 格式不正确';if(notify)this.$message.error(msg);return msg}
			if(this.form.client&&this.form.client!==client){const msg=`广告位的发布商 ID 与全局配置不一致：${client}`;if(notify)this.$message.error(msg);return msg}
			if(!this.form.client)this.form.client=client;row.slot=slot;row.format=ins.getAttribute('data-ad-format')||'auto';row.layout=ins.getAttribute('data-ad-layout')||'';row.layoutKey=ins.getAttribute('data-ad-layout-key')||'';
			const fw=ins.getAttribute('data-full-width-responsive');row.fullWidth=fw==null?null:fw==='true';const match=(ins.getAttribute('style')||'').match(/text-align\s*:\s*(left|center|right)/i);row.alignment=match?match[1].toLowerCase():'center';
			if(notify)this.$message.success('广告单元代码识别成功');return''
		},
		validate(){if(this.form.client&&!/^ca-pub-\d+$/.test(this.form.client))return'发布商 ID 格式应为 ca-pub-数字';const active=this.form.autoAds||this.form.slots.some(i=>i.enabled);if(this.form.enabled&&active&&!this.form.client)return'启用自动广告或手动广告位前请填写发布商 ID';const ps=new Set();for(const i of this.form.slots){if(!i.position)return'请为每个广告位选择位置';if(ps.has(i.position))return`广告位置不能重复：${i.position}`;ps.add(i.position);if(i.enabled){const e=this.parseSlotCode(i);if(e)return`${i.position}：${e}`}}return''},
		submit(){const e=this.validate();if(e){this.$message.error(e);return}const payload={version:1,enabled:this.form.enabled,autoAds:this.form.autoAds,client:this.form.client,adsTxt:this.form.adsTxt,slots:this.form.slots.map(i=>({name:i.name||'',slot:i.slot,position:i.position,enabled:i.enabled,format:i.format||'auto',layout:i.layout||'',layoutKey:i.layoutKey||'',fullWidth:i.fullWidth,alignment:i.alignment||'center'}))};this.saving=true;updateAdsenseConfig(payload).then(()=>{this.$message.success('广告配置保存成功');this.getData()}).finally(()=>{this.saving=false})}
	}}
</script>

<style scoped>
	.card-header,.section-heading,.section-action,.master-switch,.slot-card-head{display:flex;align-items:center}.card-header,.section-action,.master-switch,.slot-card-head{justify-content:space-between;gap:12px}.card-title{margin-right:12px;font-weight:600}.card-tip,.field-tip,.section-heading p,.master-switch p{color:#909399;font-size:12px}.adsense-form{margin-top:18px}.master-switch{margin-bottom:16px;padding:18px 22px;border:1px solid #b3d8ff;border-radius:8px;background:#ecf5ff}.master-switch p{margin:5px 0 0}.config-section{padding:20px 22px;border:1px solid #ebeef5;border-radius:8px}.config-section+.config-section{margin-top:16px}.section-heading{gap:12px;margin-bottom:18px}.section-action .section-heading{margin-bottom:18px}.section-index{display:inline-flex;width:28px;height:28px;flex:0 0 28px;align-items:center;justify-content:center;border-radius:50%;background:#ecf5ff;color:#409eff;font-weight:600}.section-heading h3{margin:0;color:#303133;font-size:15px}.section-heading p{margin:4px 0 0}.normal-input{width:440px;max-width:100%}.position-select{width:280px;max-width:100%}.ads-textarea{width:680px;max-width:100%}.field-tip{margin-top:7px;line-height:1.8}.slot-list{display:grid;gap:14px}.slot-card{padding:16px 18px;border:1px solid #dcdfe6;border-radius:6px;background:#fafafa}.slot-card-head{margin-bottom:16px}.slot-card .el-form-item{margin-bottom:15px}.parse-result{display:flex;flex-wrap:wrap;gap:8px;color:#67c23a;font-size:12px}.slots-tip{margin-top:10px}.empty-slots{padding:28px;border:1px dashed #dcdfe6;border-radius:6px;background:#fafafa;color:#909399;text-align:center}.danger-text{margin-left:12px;color:#f56c6c}code{padding:2px 5px;border-radius:3px;background:#f2f4f7;color:#d14}@media(max-width:768px){.config-section{padding:16px 12px}.section-action{align-items:flex-start;flex-direction:column}.card-tip{display:none}}
</style>
