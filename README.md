Test project to contains knowledge
==================================

## Benchmark tests
To run a test multiple times and measure performances of methods:

``` bash
jmh:run -prof jmh.extras.JFR -t1 -f 1 -wi 10 -i 20 .*TestBenchmark.*
```

