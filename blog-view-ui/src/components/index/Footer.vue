<template>
	<footer class="site-footer" :class="{'compact-footer': compact}">
		<div class="footer-container">
			<div class="footer-main" v-if="!compact">
				<div class="footer-col footer-about">
					<h4 class="footer-title">{{ siteInfo.blogName || 'BlogLoom' }}</h4>
					<p class="footer-desc">{{ description }}</p>
					<p class="footer-motto" v-if="hitokoto">“{{ hitokoto }}”</p>
					<div class="footer-social" v-if="socialLinks.length">
						<a v-for="link in socialLinks" :key="link.nameEn" class="social-link"
						   :href="link.url" target="_blank" rel="noopener noreferrer" :title="link.nameZh">
							{{ link.nameZh }}
						</a>
					</div>
				</div>

				<div class="footer-col">
					<h4 class="footer-title">最新博客</h4>
					<ul class="footer-list">
						<li v-for="item in newBlogList" :key="item.id">
							<a href="javascript:;" @click.prevent="toBlog(item)">{{ item.title }}</a>
						</li>
						<li v-if="!newBlogList.length" class="footer-empty">暂无文章</li>
					</ul>
				</div>

				<div class="footer-col">
					<h4 class="footer-title">快捷链接</h4>
					<ul class="footer-list">
						<li v-for="link in quickLinks" :key="link.path">
							<router-link :to="link.path">{{ link.label }}</router-link>
						</li>
					</ul>
				</div>
			</div>

			<div class="footer-divider"></div>

			<div class="footer-bottom">
				<p class="footer-copyright">
					<span v-if="siteInfo.copyright" class="footer-copyright-title">{{ siteInfo.copyright.title }}</span>
					<a v-if="siteInfo.copyright" :href="siteInfo.copyright.siteUrl || '/'" class="footer-link">{{ siteInfo.copyright.siteName }}</a>
					<template v-if="siteInfo.copyright && siteInfo.beian"><span class="footer-sep">|</span></template>
					<a v-if="siteInfo.beian" class="footer-link" rel="external nofollow noopener" href="https://beian.miit.gov.cn/" target="_blank">{{ siteInfo.beian }}</a>
				</p>

				<div class="footer-badges">
					<div class="github-badge" v-for="(item,index) in badges" :key="index">
						<a rel="external nofollow noopener" :href="item.url" target="_blank" :title="item.title">
							<span class="badge-subject">{{ item.subject }}</span>
							<span class="badge-value" :class="`bg-${item.color}`">{{ item.value }}</span>
						</a>
					</div>
				</div>
			</div>
		</div>
	</footer>
</template>

<script>
	export default {
		name: "Footer",
		props: {
			siteInfo: {
				type: Object,
				required: true
			},
			badges: {
				type: Array,
				required: true
			},
			newBlogList: {
				type: Array,
				required: true
			},
			introduction: {
				type: Object,
				default: () => ({})
			},
			hitokoto: {
				type: String,
				required: true
			},
			compact: {
				type: Boolean,
				default: false
			}
		},
		data() {
			return {
				//静态数据：站点简介，后续可收敛到系统配置
				description: '记录技术成长与生活思考，分享原创文章与开源项目。',
				quickLinks: [
					{label: '首页', path: '/home'},
					{label: '归档', path: '/archives'},
					{label: '动态', path: '/moments'},
					{label: '友人帐', path: '/friends'},
					{label: '关于我', path: '/about'}
				],
				socialLabelMap: {
					github: 'GitHub',
					bilibili: 'bilibili',
					telegram: 'Telegram',
					qq: 'QQ',
					netease: '网易云音乐',
					email: '邮箱'
				}
			}
		},
		computed: {
			socialLinks() {
				const links = []
				Object.keys(this.socialLabelMap).forEach(nameEn => {
					const url = this.introduction && this.introduction[nameEn]
					if (this.hasLink(url)) {
						links.push({nameEn, nameZh: this.socialLabelMap[nameEn], url})
					}
				})
				return links
			}
		},
		methods: {
			toBlog(blog) {
				this.$store.dispatch('goBlogPage', blog)
			},
			hasLink(value) {
				if (typeof value !== 'string') return false
				const link = value.trim()
				return link.length > 0
					&& link !== '#'
					&& link.toLowerCase() !== 'null'
					&& link.toLowerCase() !== 'undefined'
					&& !/^javascript:/i.test(link)
			}
		}
	}
</script>

<style scoped>
	@import "../../assets/css/badge.css";

	.site-footer {
		margin-top: 40px;
		padding: 48px 0 24px;
		color: #9aa5b4;
		background: linear-gradient(180deg, #1f2733 0%, #171d27 100%);
		font-size: 13px;
		line-height: 1.7;
	}

	.footer-container {
		max-width: 1127px;
		margin: 0 auto;
		padding: 0 16px;
	}

	.footer-main {
		display: grid;
		grid-template-columns: 2.2fr 1.4fr 1fr;
		gap: 32px;
	}

	.footer-title {
		margin: 0 0 16px;
		color: #f4f6f8;
		font-size: 15px;
		font-weight: 600;
		letter-spacing: .4px;
	}

	.footer-about .footer-title { font-size: 18px; }

	.footer-desc {
		margin: 0 0 10px;
		color: #8b96a5;
	}

	.footer-motto {
		margin: 0 0 16px;
		color: #76828f;
		font-style: italic;
	}

	.footer-social {
		display: flex;
		flex-wrap: wrap;
		gap: 8px;
	}

	.social-link {
		padding: 3px 12px;
		border: 1px solid rgba(255, 255, 255, .14);
		border-radius: 999px;
		color: #b9c2cd !important;
		font-size: 12px;
		transition: all .2s;
	}

	.social-link:hover {
		border-color: #5eb8ff;
		background: rgba(94, 184, 255, .12);
		color: #5eb8ff !important;
		transform: translateY(-1px);
	}

	.footer-list {
		margin: 0;
		padding: 0;
		list-style: none;
	}

	.footer-list li {
		margin-bottom: 9px;
	}

	.footer-list a {
		color: #98a3b1 !important;
		transition: color .2s, padding-left .2s;
	}

	.footer-list a:hover {
		padding-left: 4px;
		color: #5eb8ff !important;
	}

	.footer-empty {
		color: #5f6a77;
		font-style: italic;
	}

	.footer-divider {
		margin: 36px 0 20px;
		height: 1px;
		background: rgba(255, 255, 255, .08);
	}

	.footer-bottom {
		display: flex;
		align-items: center;
		justify-content: space-between;
		flex-wrap: wrap;
		gap: 12px;
	}

	.footer-copyright {
		margin: 0;
		color: #77828f;
		font-size: 12px;
	}

	.footer-copyright-title {
		margin-right: 8px;
	}

	.footer-link {
		color: #5eb8ff !important;
	}

	.footer-sep {
		margin: 0 10px;
		color: #4b5563;
	}

	.footer-badges {
		display: flex;
		align-items: center;
		flex-wrap: wrap;
		gap: 8px;
	}

	.github-badge a { color: #fff; }

	.compact-footer { margin-top: 0; padding: 14px 0; }
	.compact-footer .footer-divider { margin: 8px 0 14px; }
</style>

<!-- 移动端样式（独立文件，单独维护） -->
<style scoped src="@/styles/mobile/components/footer.mobile.scss" lang="scss"></style>
