cd ../python-modules/baselines-master
python -m baselines.run --alg=acktr --env=Diplomacy-v0 --save_path=./diplomacy.pkl --num_timesteps=20