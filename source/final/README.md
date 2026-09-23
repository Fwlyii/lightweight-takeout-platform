# 实验报告 2.0 版本

本目录是可独立编译的 LaTeX 实验报告源码包，包含主文件、正文、图片和格式配置；编译结果单独存放在仓库的 `docs/final.pdf`。

## 目录结构

- `tjumain.tex`：主 LaTeX 文件
- `body/`：报告正文与软件需求规格说明书正文
- `figures/`：正文引用的图片和图表
- `setup/`：宏包、命令、格式和封面信息
- 编译结果已迁移到仓库的 `docs/final.pdf`

## 编译方式

推荐使用 XeLaTeX：

```text
xelatex tjumain.tex
xelatex tjumain.tex
```

也可以使用 Tectonic：

```text
tectonic tjumain.tex
```

需要至少编译两遍时，由编译工具自动或手动完成，以更新目录和交叉引用。报告会优先使用系统中的华文字体；如果系统没有模板首选字体，将使用源码中配置的 Fandol 字体回退方案。

## 注意事项

- 编译时请将工作目录切换到本目录，不要只打开某个子文件单独编译。
- PDF 是二进制文件，应使用 PDF 阅读器或编辑器的 PDF 预览功能打开，不能按普通文本文件查看。
- `.aux`、`.log`、`.out`、`.toc` 和 `.synctex.gz` 属于编译缓存，本源码包未保留这些文件。
