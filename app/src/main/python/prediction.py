import pandas as pd
from sklearn.metrics import f1_score
from sklearn.model_selection import StratifiedKFold
from sklearn.model_selection import train_test_split
from sklearn.tree import DecisionTreeClassifier
from sklearn.ensemble import RandomForestClassifier
import numpy as np
from sklearn.preprocessing import LabelEncoder


def find_path(node_numb, path, x):

  path.append(node_numb)

  if node_numb == x:
    return True

  left = False
  right = False

  if (children_left[node_numb] !=-1):
    left = find_path(children_left[node_numb], path, x)

  if (children_right[node_numb] !=-1):
    right = find_path(children_right[node_numb], path, x)

  if left or right :
    return True

  path.remove(node_numb)
  return False

def get_rule(path, column_names):

  mask = ''
  for index, node in enumerate(path):
    #We check if we are not in the leaf
    if index!=len(path)-1:
      # Do we go under or over the threshold ?
      if (children_left[node] == path[index+1]):
        mask += "(df['{}']<= {}) \t ".format(column_names[feature[node]], threshold[node])
      else:
        mask += "(df['{}']> {}) \t ".format(column_names[feature[node]], threshold[node])

  # We insert the & at the right places
  mask = mask.replace("\t", "&", mask.count("\t") - 1)
  mask = mask.replace("\t", "")

  return mask



"""
Change the type of the variables from categorical to numerical

@data - Data to be processed. Can contain both categorical or 
numerical variables.

"""
def categorical_numer(data):
    # Se extraen las categorias
    columnas_categoricas = list(data.select_dtypes('object').astype(str))
    variables_categoricas = data[columnas_categoricas]
    
    # Eliminamos estas variables
    data = data.drop(columns = columnas_categoricas)
    #Aplicamos label encoder
    data_cat = variables_categoricas.apply(preprocessing.LabelEncoder().fit_transform)
    #juntamos el conjunto de train
    processed_data = pd.concat((data, data_cat), axis=1, join='outer', ignore_index=False,
                                   levels=None, names=None, verify_integrity=False, copy=True)
    
    return processed_data


def validacion_cruzada(modelo, X, y, cv):
  y_test_all = []
  mean_score = 0
  
  for train, test in cv.split(X, y):
    modelo = modelo.fit(X[train],y[train])
    y_pred = modelo.predict(X[test])
    y_test_all = np.concatenate([y_test_all,y[test]])
    
    # With the average 'micro', it calculates metrics globally by counting 
    # the total true positives, false negatives and false positives.
    mean_score += f1_score(y[test], y_pred, average='micro')
    pass

  mean_score = mean_score / 5 # number of splits

  
  return modelo, y_test_all, mean_score


"""
This function train the model, and predict if the user is in a risk situation currently

@data - All the glucemic data of the user
@info - Current glucemic data of the user

"""

def main(data_history, current_data):
  
  X = pd.read_csv(data_history, sep=',', engine='c') # engine='c' reads much faster
  X_tst = pd.read_csv(current_data, sep=",", engine='c')

  labels = X['class']
  y = np.ravel(labels.values)

  # Delete unused columns
    
  X = X.values
  X_tst = X_tst.values

  # Cross validator
  skf = StratifiedKFold(n_splits = 5, shuffle=True, random_state=123456)
  #Classifier
  #rf = RandomForestClassifier(n_estimators = 200, n_jobs=-1)
  rf = DecisionTreeClassifier()

  #from sklearn.experimental import enable_hist_gradient_boosting  # noqa
  #from sklearn.ensemble import HistGradientBoostingClassifier
  #hg = HistGradientBoostingClassifier()

  rf, y_test_rf, score = validacion_cruzada(rf, X, y, skf)
  
  clf = rf
  clf = clf.fit(X, y)

  """"
  from com.chaquo.python import Python
  context = Python.getPlatform().getApplication()
  external_files_dir = context.getExternalFilesDir()
  """

  # y_pred_tra = clf.predict(X)
  # print("F1 score (tra): {:.4f}".format(f1_score(y, y_pred_tra,average='micro')))

  risk = clf.predict(X_tst)

  string_score = str(score)
  #importances = clf.feature_importances_
  
  importances_ = {} # This dictionay will contain de importances joined to the name of the variables
  support = ""

  # To better understanding the model (the response from the model)
  for feat, importance in zip(X.columns, clf.feature_importances_):
    support += support + feat + ":" + str(importance*100) + ","
    pass

  # Write the model to maintain the persistence
  import pickle
  pickle.dump( clf, open( "classifier.pickle", "wb" ) )

  #from joblib import dump    # More efficient
  #dump(clf, 'classifier.pickle')

  n_nodes = clf.tree_.node_count
  children_left = clf.tree_.children_left
  children_right = clf.tree_.children_right
  feature = clf.tree_.feature
  threshold = clf.tree_.threshold

  # Leaves
  leave_id = clf.apply(X_tst)
  paths ={}

  for leaf in np.unique(leave_id):
    path_leaf = []
    find_path(0, path_leaf, leaf)
    paths[leaf] = np.unique(np.sort(path_leaf))

  rules = {}

  for key in paths:
    rules[key] = get_rule(paths[key], X.columns)

  result = string_score + "," + risk + "," + rules + "," + support

  return result
  pass


def predictFromModel(data):
  X_tst = pd.read_csv(data, sep=",", engine='c')

  from os.path import dirname, join
  f = open(join(dirname(file), 'classifier.pickle'), 'rb')
  import pickle

  #Classifier
  clf = pickle.load(f)
  risk = clf.predict(X_tst)

  return risk
  pass


def find_path(node_numb, path, x):

  path.append(node_numb)

  if node_numb == x:
    return True

  left = False
  right = False

  if (children_left[node_numb] !=-1):
    left = find_path(children_left[node_numb], path, x)

  if (children_right[node_numb] !=-1):
    right = find_path(children_right[node_numb], path, x)

  if left or right :
    return True

  path.remove(node_numb)
  return False

def get_rule(path, column_names):

  mask = ''
  for index, node in enumerate(path):
    #We check if we are not in the leaf
    if index!=len(path)-1:
      # Do we go under or over the threshold ?
      if (children_left[node] == path[index+1]):
        mask += "(df['{}']<= {}) \t ".format(column_names[feature[node]], threshold[node])
      else:
        mask += "(df['{}']> {}) \t ".format(column_names[feature[node]], threshold[node])

  # We insert the & at the right places
  mask = mask.replace("\t", "&", mask.count("\t") - 1)
  mask = mask.replace("\t", "")

  return mask