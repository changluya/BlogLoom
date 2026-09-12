package top.naccl.model.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description: 侧边栏资料卡
 * @Author: changlu
 * @Date: 2026-09-13
 */
@NoArgsConstructor
@Getter
@Setter
@ToString
public class Introduction {
	private String avatar;
	private String name;
	private String github;
	private String telegram;
	private String qq;
	private String bilibili;
	private String netease;
	private String email;

	private List<String> rollText = new ArrayList<>();
	private List<Favorite> favorites = new ArrayList<>();

}
