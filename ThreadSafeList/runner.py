import subprocess as sp

basic_cmd = ["/usr/bin/time", "-f", "\"%e\"", "java", "-jar", "lock-free.jar"]

for th_num in ["2", "8", "16"]:
    for ops_num in [str(10**6), str(10**7)]:
        no_type_cmd = basic_cmd + [th_num, th_num, ops_num]

        sp.check_output(no_type_cmd + ["0"])
        sp.check_output(no_type_cmd + ["1"])