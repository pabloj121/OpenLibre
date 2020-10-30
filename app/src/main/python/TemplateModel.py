# -*- coding: utf-8 -*-
"""
Spyder Editor

This is a temporary script file.
"""


"""
Este archivo se ejecutará solamente cuando el usuario realice
un escaneo. Si no hay datos suficientes, se informa.

la variable ultima del Y es la que debe comprobarse para darle 
información al usuario, así como el soporte del arbol de decision
que razone si hay peligro.
"""





#import tensorflow as tf
#from tensorflow import keras
#from tensorflow.contrib import lite
import pandas as pd
import lightgbm as lgb
from sklearn.metrics import f1_score
from sklearn.model_selection import StratifiedKFold
from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestClassifier
import numpy as np

skf = StratifiedKFold(n_splits = 5, shuffle=True, random_state=123456)

def validacion_cruzada(modelo, X, y, cv):
  y_test_all = []

  for train, test in cv.split(X, y):
    #t = time.time()
    modelo = modelo.fit(X[train],y[train])
    #tiempo = time.time() - t
    y_pred = modelo.predict(X[test])
    #print("F1 score (val): {:.4f}".format(f1_score(y[test],y_pred,average='micro')))
    y_test_all = np.concatenate([y_test_all,y[test]])

  #print("")

  return modelo, y_test_all, f1_score(y[test],y_pred,average='micro')

# Its necessary to read and organize the data as a CSV file
def main(data, info):
  df = pd.read_csv(data, sep=',', engine='c') #engine='c'
  #print(df)

  # split the data into train and test set
  #train, test = train_test_split(df, test_size=0.2, random_state=123456, shuffle=True)

  #lgbm = lgb.LGBMClassifier(objective='regression_l1', n_estimators=500,n_jobs=-1)
  #lgbm, y_test_lgbm, score = validacion_cruzada(lgbm,X,y,skf)
  #lgbm, y_test_lgbm = validacion_cruzada(lgbm,X,y,skf)

  rf = RandomForestClassifier(n_estimators = 200, n_jobs=-1)
  #rf = RandomForestClassifier()

  #from sklearn.experimental import enable_hist_gradient_boosting  # noqa
  #from sklearn.ensemble import HistGradientBoostingClassifier
  #hg = HistGradientBoostingClassifier()



  rf, y_test_rf, score = validacion_cruzada(rf, X, y, skf)
  string_score = str(score)

  result = string_score + "," + risk + "," + "support"
  #result = "puntuacion,riesgo,explicaciones"
  return result

  pass











#x_train = pd.read_csv('archivo.csv', sep=",", index_col=0)
#y_train = x_train.pop('class')


# Borrar columnas que no sean útiles !


# Exploracion de los datos
#x_train.head()
#x_train.shape[0] #x_test.shape[0]

# x_train['sport'].value_counts().plot(kind='barh')
# plt.show

# ES IMPORTANTE EXPLORAR LOS DATOS ANTES DE CONSTRUIR EL MODELO


# CREAR COLUMNAS DE CARACTERISTICAS Y FUNCIONES DE ENTRADA
CATEGORICAL_COLUMNS = [ 'timezone', 'date', 'lunchtime', 'food_type', 'sport', 'trend', 'stress']
NUMERIC_COLUMNS = ['id']
	
tf.random.set_seed(123456)

def one_hot_cat_column(feature_name, vocab):
  return tf.feature_column.indicator_column(
      tf.feature_column.categorical_column_with_vocabulary_list(feature_name,
                                                 vocab))
feature_columns = []

for feature_name in CATEGORICAL_COLUMNS:
  # Need to one-hot encode categorical features.
  vocabulary = x_train[feature_name].unique()
  feature_columns.append(one_hot_cat_column(feature_name, vocabulary))

for feature_name in NUMERIC_COLUMNS:
  feature_columns.append(tf.feature_column.numeric_column(feature_name,
                                           dtype=tf.float32))


# A continuacion se crean las funciones de entrada
# Use entire batch since this is such a small dataset.
NUM_EXAMPLES = len(y_train)

def make_input_fn(X, y, n_epochs=None, shuffle=True):
  def input_fn():
    dataset = tf.data.Dataset.from_tensor_slices((dict(X), y))
    if shuffle:
      dataset = dataset.shuffle(NUM_EXAMPLES)
    # For training, cycle thru dataset as many times as need (n_epochs=None).
    dataset = dataset.repeat(n_epochs)
    # In memory training doesn't use batching.
    dataset = dataset.batch(NUM_EXAMPLES)
    return dataset
  return input_fn

# Training and evaluation input functions.
train_input_fn = make_input_fn(x_train, y_train)
# test_input_fn = make_input_fn(X_train, y_train, num_epochs=1, shuffle=False)

# Inspeccionar el conjunto de datos
ds = make_input_fn(x_train, y_train, batch_size=10)()

# Exploración de los datos
for feature_batch, label_batch in ds.take(1):
  print('Some feature keys:', list(feature_batch.keys()))
  print()
  print('A batch of class:', feature_batch['class'].numpy())
  print()
  print('A batch of Labels:', label_batch.numpy())



params = {
	'n_trees': 50,
	'max_depth': 3,
	'n_batches_per_layer': 1,
	'center_bias': True
}


# IMPLEMENTACION Boosted Trees+

# Since data fits into memory, use entire dataset per layer. It will be faster.
# Above one batch is defined as the entire dataset.
n_batches = 1
model = tf.estimator.BoostedTreesClassifier(feature_columns,
                                          n_batches_per_layer=n_batches)

# model = tf.estimator.BoostedTreesClassifier(feature_columns, **params)

# The model will stop training once the specified number of trees is built, not
# based on the number of steps.
model.train(train_input_fn, max_steps=100)

# EVALUATION OF THE MODEL. ONLY SEPARATING DATA INTO TRAINING AND TEST
# result = model.evaluate(test_input_fn) # test_input_fn solo en caso que separemos train y test
# clear_output()
# print(pd.Series(result))



# Leo datos de entrenamiento
# Separo la variable a predecir
# Obtenemos el modelo, ya podemos subirlo a la nube


def serving_input_receiver_fn():
  features = { 'x': tf.placeholder(
    shape=[1, 28, 28, 1], # input shape with batch size 1
    dtype=tf.float64) 
  }  
  
  return tf.estimator.export.ServingInputReceiver(
    features, 
    features
  )


model_dir = "/somedir/" # location of model

from model import model_fn

model = tf.estimator.Estimator( 
  model_dir=model_dir,
  model_fn = model_fn
)

model.export_saved_model(model_dir, serving_input_receiver_fn)


converter = tf.lite.TFLiteConverter.from_saved_model(model_dir)
tflite_model = converter.convert()
open("model.tflite", "wb").write(tflite_model)


# tflite_convert --saved_model_dir=/somedir/123456 --input_shapes=1,28,28,1 --input_arrays=Placeholder --output_arrays=softmax_tensor --output_file=/model.tflite

""" 
# Write out the model file
model_file = "model.tf"
model_file.models.save(model, model_file)


# Convert the Model file to TensorFlow Lite
converter = lite.TocoConverter.from_keras_model_file(model_file)
tflite_model = converter.convert()
open("model.tflite", "wb").write(tflite_model)
"""


"""

# + GRAFICAS APARTE DEL ROC CURVE ?!

from sklearn.metrics import roc_curve
from matplotlib import pyplot as plt

fpr, tpr, _ = roc_curve(y_eval, probs)
plt.plot(fpr, tpr)
plt.title('ROC curve')
plt.xlabel('false positive rate')
plt.ylabel('true positive rate')
plt.xlim(0,)
plt.ylim(0,)

"""