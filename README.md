# 介绍

## 前言

读java native源代码时，我们一般会去网站下载[openjdk8源码（http://download.java.net/openjdk/jdk8/promoted/b132/openjdk-8-src-b132-03_mar_2014.zip](http://download.java.net/openjdk/jdk8/promoted/b132/openjdk-8-src-b132-03_mar_2014.zip) ，然后进行分析，随后很多文章会让你编译。其实，如果只是为了分析源码，完全不用编译，直接找到hotspot文件夹下的src文件夹查看即可，这里就是hotspot的源码。

*注意：官网的源码下载地址写错了，它写成了http://www.java.net/download/openjdk/jdk8/promoted/b132/openjdk-8-src-b132-03_mar_2014.zip*

## 存在的问题

源码中的include 路径不全，导致很多函数无法跳转，只得手动查找，很不方便。以 `./src/cpu/sparc/vm/assembler_sparc.cpp`文件为例，原始代码是这样的：

```
#include "precompiled.hpp"
#include "asm/assembler.hpp"
#include "asm/assembler.inline.hpp"

int AbstractAssembler::code_fill_byte() {
  return 0x00;                  // illegal instruction 0x00000000
}
```

我们希望是这样的：

```
#include "./src/share/vm/precompiled/precompiled.hpp"
#include "./src/share/vm/asm/assembler.hpp"
#include "./src/share/vm/asm/assembler.inline.hpp"

int AbstractAssembler::code_fill_byte() {
  return 0x00;                  // illegal instruction 0x00000000
}
```

## 分析问题&解决方案

当然，我们可以手动寻找对应的头文件，然后一个一个补全；但是文件数量有上千个，这样会耗费许多时间，也因此有了本项目——自动补全include中的路径。查看不同文件中的include路径，发现有两大类，一类是`include "precompiled.hpp"`，一类是包含在文件夹`./src/share/vm`下的各个头文件，如`asm`、`prims`、`oop`、`utilities`等，因此解决步骤如下：

1. 对于`"precompiled.hpp"，我们可以直接用编辑器将其全部替换为`include "./src/share/vm/precompiled/precompiled.hpp"`;
2. 对于其他诸如`include "asm/assembler.hpp"`、`#include "oops/oop.inline.hpp"`、`#include "utilities/accessFlags.hpp"`等依赖不同文件路径的头文件，利用本项目中的程序自动将其补全。

## 实现原理

1. 遍历src文件夹下的所有文件，找出后缀为`cpp`、`hpp`、`c`、`h`的文件，将其保存在List中；同时将对应的文件夹路径保存在Set中（之所以选择Set而不是List是因为同一文件夹下可能有很多文件，而文件夹保存一次就好）；

2. 处理List中保存的文件，每个文件处理时读取Set中匹配的文件夹，将诸如`#include "asm`等形式替换成`#include "./src/share/vm/asm`，即可。

   ​

## 其他问题

还有一些文件依赖于特定的平台，如`./src/share/vm/utilities/copy.hpp`中：

```
#ifdef TARGET_ARCH_x86
# include "copy_x86.hpp"
#endif
#ifdef TARGET_ARCH_sparc
# include "copy_sparc.hpp"
#endif
#ifdef TARGET_ARCH_zero
# include "copy_zero.hpp"
#endif
#ifdef TARGET_ARCH_arm
# include "copy_arm.hpp"
#endif
#ifdef TARGET_ARCH_ppc
# include "copy_ppc.hpp"
#endif
```

此种代码程序并未处理，因此依赖平台的函数还是有可能找不到，有需要的可以将程序中的

`static String startInclude = "#include \"";`替换为`static String startInclude = "# include \"";`再运行一次即可（注意#和include之间多了一个空格）。

## 运行须知

本程序运行时，建议将hotspot的源码目录src单独放在工程下的一个文件夹（本项目中为`/Resource`）中，防止混乱。