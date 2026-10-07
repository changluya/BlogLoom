'use strict';

/**
 * 微信公众号选择器集中维护（参照 auto-sync-blog 已跑通的 Selenium 实现）。
 * 站点改版时**只改本文件**，index.js 的业务流程保持不变。
 *
 * 关键点：
 * - 登录态：已登录时首页出现 `.weui-desktop_name`（账号名），未登录时出现 `#jumpUrl` 登录入口；
 * - 编辑器：图文消息编辑页正文是 ProseMirror（`.ProseMirror`）；
 * - 正文样式：公众号编辑器不识别 Markdown，需先用 md.doocs.org 转成带样式 HTML 再粘贴；
 * - 发布配置：封面/摘要/原创/合集/创作来源都在编辑页底部 `.appsmg-editor__after-area` 与 `#article_setting_area2`；
 * - 发表：底部 `.mass_send` → 群发弹窗 `.new_mass_send_dialog` → 二次确认 `.double_check_dialog`。
 */

const SELECTORS = {
  // ---- 首页 / 登录态 ----
  accountName: '.weui-desktop_name',
  jumpUrl: '#jumpUrl',

  // ---- 进入编辑器（内容管理 → 草稿箱 → 新的创作 → 文章）----
  menuCreate: '.weui-desktop-menu_create',
  subMenuItem: '.weui-desktop-sub-menu__item',
  newCreateButton: 'button:has-text("新的创作"), .weui-desktop-btn_wrp:has-text("新的创作")',
  createArticleItem: '.weui-desktop-dropdown__list-ele:has-text("文章")',

  // ---- 编辑器：标题 / 作者 ----
  titlePanel: '#js_title_main',
  titleInput: '.title-editor__input .ProseMirror',
  titleFallbackInput: '#js_title_main #title, #title',
  authorArea: '#js_author_area',
  authorInput: '#js_author_area #author, #author',

  // ---- 编辑器：正文（内容 ProseMirror，注意与标题 ProseMirror 区分） ----
  editor: '.rich_media_content .ProseMirror, #ueditor_0 .ProseMirror, .mock-iframe-body .ProseMirror',
  continueInsert: 'button:has-text("继续插入")',
  insertProfileBtn: '#js_editor_insertProfile',
  profileDialog: '.weui-desktop-dialog__wrp.profile_dialog',
  profileSearch: '.weui-desktop-search.weui-desktop-search_btn-appendIn',
  profileSearchBtn: '.weui-desktop-search__btn',
  profileGridCol: '.weui-desktop-grid__col',
  profileCardBody: '.wx_profile_card_bd',

  // ---- 通用弹窗 ----
  dialog: '.weui-desktop-dialog:visible',
  dialogFt: '.weui-desktop-dialog__ft',
  btnWrp: '.weui-desktop-btn_wrp',
  btnPrimary: '.weui-desktop-btn_primary',

  // ---- 发布配置：封面 + 摘要 ----
  afterArea: '.appsmg-editor__after-area',
  coverDescriptionArea: '#js_cover_description_area',
  coverArea: '#js_cover_area',
  coverNullPop: '.js_cover_null_pop',
  selectCoverFromContent: '.js_selectCoverFromContent',  coverPreview: '.js_cover_preview_new.select-cover__preview.first_appmsg_cover',
  imgCropPanel: '.img_crop_panel',
  contentImgItem: '.appmsg_content_img_item',
  coverNext: 'button:has-text("下一步")',
  coverDone: 'button:has-text("确认"), button:has-text("完成"), button:has-text("确定")',
  descTextarea: '#js_description_area textarea',

  // ---- 发布配置：赞赏 / 付费 / 留言（默认开关） ----
  rewardArea: '#js_reward_setting_area',
  rewardInput: '#js_reward_setting_area input.js_reward_setting_checkbox',
  rewardSwitch: '#js_reward_setting_area .js_reward_open, #js_reward_setting_area .setting-group__switch',
  payArea: '#js_pay_setting_area',
  payInput: '#js_pay_setting_area input.js_pay_setting_checkbox',
  paySwitch: '#js_pay_setting_area .js_pay_open, #js_pay_setting_area .setting-group__switch',
  rewardTypeAuthor: '.weui-desktop-form__check-label:has-text("赞赏作者"), label:has-text("赞赏作者")',
  rewardAccountItem: '.reward-account-setting .weui-desktop-form__check-label, .reward-account-setting label',
  rewardAgreementInput: '.weui-desktop-dialog input[type="checkbox"].weui-desktop-form__checkbox',
  rewardAgreementLabel: '.weui-desktop-dialog label:has-text("我已阅读")',
  rewardDialogConfirm: 'button.weui-desktop-btn_primary:has-text("确定"), button:has-text("确定")',
  paySingleLabel: '.weui-desktop-form__check-label:has-text("单篇付费"), label:has-text("单篇付费")',
  payAmountInput: 'input.weui-desktop-form__input[placeholder*="整数"], input[placeholder*="1-10000"]',
  payDialogConfirm: 'button.weui-desktop-btn_primary:has-text("确定"), button:has-text("确定")',
  commentArea: '#js_comment_and_fansmsg_area',

  // ---- 发布配置：原创 ----
  originalBox: '#js_original_box',
  original: '#js_original',
  originalTypeText: 'label:has-text("文字原创"), .weui-desktop-form__check-label:has-text("文字原创")',
  originalTypeRadio: '.js_original_type_radio',
  originalAgreementInput: '.original_agreement input[type="checkbox"]',
  originalAgreementLabel: '.original_agreement label',
  originalAgreementIcon: '.original_agreement .weui-desktop-icon-checkbox',
  originalAgreement: '.original_agreement label, .original_agreement .weui-desktop-form__check-label, label:has-text("我已阅读")',
  dialogConfirm: 'button:has-text("确定"), .weui-desktop-dialog__ft .weui-desktop-btn_primary',
  dialogCancel: 'button:has-text("取消"), .weui-desktop-dialog__ft .weui-desktop-btn_default',
  formCheckbox: '.weui-desktop-form__checkbox',
  originalAgreementOld: '.original_agreement .weui-desktop-icon-checkbox',

  // ---- 发布配置：合集 / 创作来源 ----
  settingArea: '#article_setting_area2',
  articleTagsArea: '#js_article_tags_area',
  articleTagsLabel: '.allow_click_opr.js_article_tags_label',
  collectionOption: '.select-opt-li',
  claimSourceArea: '#js_claim_source_area',
  claimSourceDesc: '.js_claim_source_desc, .allow_click_opr.js_claim_source_desc',
  claimSourceOption: '.weui-desktop-form__check-label:has-text("个人观点，仅供参考"), label:has-text("个人观点，仅供参考")',
  claimConfirm: 'button.weui-desktop-btn_primary:has-text("确认"), button:has-text("确认")',

  // ---- 发表 ----
  bottomBar: '.js_bot_bar.tool_area',
  massSend: '.mass_send',
  massSendDialog: '.new_mass_send_dialog',
  massSendSwitch: '.mass-send__td .weui-desktop-switch__box',
  doubleCheckDialog: '.double_check_dialog',
  switchBox: '.weui-desktop-switch__box',
  successToast: '.weui-desktop-toast, .weui-desktop-toast__content, .mass_send_success',

  // ---- 保存草稿 ----
  saveButton: 'button:has-text("保存为草稿")',
  saveFallback: '#js_continue_save, #js_autosave',

  // ---- 草稿箱：定位与删除（行内悬停出现「编辑 / 删除」，删除有确认浮层） ----
  draftItem: '.weui-desktop-card',
  draftTitle: '.weui-desktop-publish__cover__title',
  draftDeleteLink: '.weui-desktop-link:has-text("删除")',
  confirmDelete: '.weui-desktop-popover button.weui-desktop-btn_primary:has-text("删除")',
};

module.exports = SELECTORS;
