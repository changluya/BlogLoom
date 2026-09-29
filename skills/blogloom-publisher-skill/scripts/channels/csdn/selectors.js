'use strict';

/**
 * CSDN 选择器集中维护（参照 auto-sync-blog 已跑通的实现）。
 * 站点改版时**只改本文件**，index.js 的业务流程保持不变。
 *
 * 关键点：
 * - 标题默认显示为 `.article-bar__title-display`（【无标题】），点击后才显示隐藏的 input；
 * - 正文是 `<pre class="editor__inner markdown-highlighting" contenteditable>`；
 * - 标签/摘要/分类/类型/可见范围都在点「发布文章」后的弹窗 `.modal__publish-article` 内。
 */

const SELECTORS = {
  // ---- 首页 / 登录态 ----
  homeWriteNew: '.toolbar-btn-write-new',
  homeLoginFun: '.toolbar-btn-loginfun',
  slider: '.verify-move-block',

  // ---- 编辑器 ----
  titleDisplay: '.article-bar__title-display',
  titleInput: 'input.article-bar__title',
  content: '.editor__inner.markdown-highlighting',
  coverImg: '.editor__inner img, .editor__inner .md-img',

  // ---- 保存草稿 ----
  saveDraftButton: 'button.btn-save',

  // ---- 打开发布弹窗 ----
  publishEntry: 'button.btn-publish',
  publishModal: '.modal__publish-article',

  // ---- 弹窗内：标签（tagContainer 已是 .mark_selection，子选择器不要再带 .mark_selection，
  //      否则 container.locator('.mark_selection ...') 永远匹配不到后代） ----
  tagContainer: '.mark_selection',
  tagBox: '.mark_selection_box',
  tagSelected: '.mark_selection_box_el_tag',
  tagDelete: '.mark_selection_box_el_tag .el-tag__close, .el-tag__close',
  tagAddBtn: '.tag__btn-tag',
  tagInput: '.el-autocomplete input.el-input__inner',
  tagModalClose: '.mark_selection_box .modal__close-button',

  // ---- 弹窗内：封面 ----
  coverItem: '.img-selection-item img.select-cover',
  coverConfirm: '.vicp-operate-btn',

  // ---- 弹窗内：摘要 ----
  summaryInput: '.desc-box .el-textarea__inner',

  // ---- 弹窗内：分类专栏 / 文章类型 / 可见范围（按 form-entry 定位） ----
  formEntry: '.form-entry',
  categoryExistingDelete: '.tag__btn-tag-delete',
  categoryAddBtn: '.tag__btn-tag',
  categoryOptionCheckbox: '.tag__option-chk',
  categorySelectedItem: '.tag__item-box',
  categoryNewTagName: '.tag__item-box .tag__name',
  optionInput: (value) => `input[value='${value}']`,

  // ---- 弹窗内：创作声明（下拉） ----
  creationTrigger: '.creation-statement-select input, .creation-statement-select .el-input, .creation-statement-select, .el-select',
  creationOption: '.el-select-dropdown:visible .el-select-dropdown__item, .el-select-dropdown__item:visible',

  // ---- 最终发布 ----
  finalPublishButton: '.modal__button-bar button',
  successLink: 'a.success-modal-btn[href*="/article/details/"]',

  // ---- 内容管理 / 删除 ----
  manageItem: '.article-list-item-mp',
  manageTitle: '.list-item-title',
  manageMore: '.el_mcm-dropdown .el-dropdown-link', // 行右侧「...」
  manageMenuDelete: '.el_mcm-dropdown-menu__item', // 下拉菜单项，文案「删除」
  manageDeleteLink: 'a.item-info-oper-text', // 草稿行内可能直接有「彻底删除」
  confirmButton: '.btn-msg-confirm, .el_mcm-message-box__btns button, .el-message-box__btns button, .el-dialog__footer button',
  confirmBox: '.el_mcm-message-box, .el-message-box, .el-dialog',
};

module.exports = SELECTORS;