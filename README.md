# 项目文档

## 写在前面

“开放源码”文件下存放的是论文*Practical GUI Testing of Android Applications via Model Abstraction and Refinement*的原作者的源代码。

“我的复写”文件下存放的是我基于文献方法设计和源码的理解，对一些重要类中与核心算法相关的方法的复写。

因此运行APE时的代码基本架构仍基于原作的源码。

## 重要代码解释

```java
 public Model actionRefinement(ModelAction action) {
        int version = this.version;
        long begin = SystemClock.elapsedRealtimeNanos();
        namingManager.actionRefinement(this, action);
        long end = SystemClock.elapsedRealtimeNanos();
        if (version == this.version) {
            return null;
        }
        eventCounters.logEvent(ModelEvent.ACTION_REFINEMENT);
        Logger.iformat("Action refinement takes %s ms.", TimeUnit.NANOSECONDS.toMillis(end - begin));
        return this;
    }

```

这是`Model`中的`actionRefinement`方法，它调用了`NamingManager`接口中的`actionRefinement`方法，这个方法体现在文献中的Algorithm 2，实现了每个模型动作中的细化。

在`NamingFactory`类中存在`actionRefinement`和`stateRefinement`两个方法的具体实现，两个方法的实现步骤大概相同，但在`stateRefinement`中L必须将S细化到不同的状态或将π细化到不同的模型action。代码体现如下：

```java
for (Name name : candidates) {
            String xpathStr = NamerFactory.nameToXPathString(name);
            Namelet currentNamelet = checkNamelet(currentNaming, name, tts1, tts2);
            if (currentNamelet == null) {
                continue;
            }
            Namer currentNamer = name.getNamer();
            List<Namer> refinedNamers = NamerFactory.getSortedAbove(currentNamer);
            List<Namer> upperBounds = new ArrayList<>();
            Set<Namer> visited = new HashSet<Namer>();
            visited.add(currentNamer);
            LinkedList<Namer> queue = new LinkedList<Namer>();
            collectSortedAbove(currentNamer, queue, visited);
            outer: for (Namer refined : refinedNamers) {
                if (!upperBounds.isEmpty()) {
                    for (Namer upper : upperBounds) {
                        if (refined.refinesTo(upper)) {
                            continue outer; // 不进行重试
                        }
                    }
                }
                Namelet newNamelet = new Namelet(xpathStr, refined);
                Naming newNaming = currentNaming.extend(currentNamelet, newNamelet);
                if (!checkStateRefinement(newNaming, refined, tts1, tts2, upperBounds)) {
                    continue;
                }
                if (!checkPredicate(nm, affected, newNaming)) {
                    continue;
                }
                results.add(new RefinementResult(false, currentNaming, newNaming, currentNamelet, newNamelet, st1, st2, tts1, tts2));
                break;
            }
        }
```

## 运行要求

push `ape.jar`

```
adb push ape.jar /data/local/tmp/
```

run jar

```
adb shell CLASSPATH=/data/local/tmp/ape.jar /system/bin/app_process /data/local/tmp/ com.android.commands.monkey.Monkey
```

run

```
./ape.py -p com.google.android.calculator --running-minutes 100 --ape sata
```

- `-p`    指定包名，与Monkey相同
- `--running-minutes`  总的测试时间。这是连续模式，即APE遇到崩溃不会停止
- `--ape sata`  使用默认探索策略