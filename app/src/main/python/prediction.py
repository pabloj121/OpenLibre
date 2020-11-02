import pandas as pd
from sklearn.metrics import f1_score
from sklearn.model_selection import StratifiedKFold
from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestClassifier
import numpy as np
from sklearn.preprocessing import LabelEncoder

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
  
  # split the data into train and test set
  #train, test = train_test_split(df, test_size=0.2, random_state=123456, shuffle=True)

  # Cross validator
  skf = StratifiedKFold(n_splits = 5, shuffle=True, random_state=123456)
  #Classifier
  rf = RandomForestClassifier(n_estimators = 200, n_jobs=-1)
  #lgbm = lgb.LGBMClassifier(objective='regression_l1', n_estimators=500,n_jobs=-1)
  
  #from sklearn.experimental import enable_hist_gradient_boosting  # noqa
  #from sklearn.ensemble import HistGradientBoostingClassifier
  #hg = HistGradientBoostingClassifier()


  rf, y_test_rf, score = validacion_cruzada(rf, X, y, skf)
  
  clf = rf
  clf = clf.fit(X, y)

  # y_pred_tra = clf.predict(X)
  # print("F1 score (tra): {:.4f}".format(f1_score(y, y_pred_tra,average='micro')))

  risk = clf.predict(X_tst)

  string_score = str(score)
  #importances = clf.feature_importances_
  
  importances_ = {} # This dictionay will contain de importances joined to the name of the variables
  support = ""

  for feat, importance in zip(X.columns, clf.feature_importances_):  
    #temp = [feat, importance*100]
    #featureImp.append(temp)
    #importances_[feat] = importance
    support += support + feat + ":" + str(importance*100) + ","
    pass

  
  result = string_score + "," + risk + "," + support
  
  return result
  pass


