import yaml
import glob
import pathlib

path = pathlib.Path().resolve()
all_files = glob.glob(str(path) + "/*.yml")   

for file in all_files:
    with open(file, 'r') as stream:
        data_loaded = yaml.safe_load(stream)
        data_loaded["locationY"] =  data_loaded["locationY"] - 64
        if "minY" in data_loaded:
            data_loaded["minY"] = data_loaded["minY"] - 64
            data_loaded["maxY"] = data_loaded["maxY"] - 64
    with open(file, 'w') as yml_file:
        yml_file.write(yaml.dump(data_loaded, sort_keys=False))