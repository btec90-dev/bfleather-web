import React, { useState, useEffect } from 'react';
import { auth, db, googleProvider, signInWithPopup, signOut, onAuthStateChanged, collection, onSnapshot, addDoc, deleteDoc, doc, updateDoc, setDoc } from './firebase';
import { Phone, Mail, MapPin, Plus, Trash2, Edit2, Shield, Calendar, Heart, FileText, CheckCircle2, DollarSign, LogOut, Sparkles } from 'lucide-react';

export default function App() {
  const [user, setUser] = useState(null);
  const [loadingAuth, setLoadingAuth] = useState(true);
  const [currentTab, setCurrentTab] = useState('dashboard');

  // Firestore sync collections
  const [careProviders, setCareProviders] = useState([]);
  const [tasks, setTasks] = useState([]);
  const [appointments, setAppointments] = useState([]);
  const [expenses, setExpenses] = useState([]);
  const [documents, setDocuments] = useState([]);

  // Dialog states
  const [showAddContact, setShowAddContact] = useState(false);
  const [newContact, setNewContact] = useState({ name: '', providerType: 'Médecin Référent', affiliation: 'RMA of New York', phone: '', email: '', address: '', notes: '', isEmergency: false, category: 'MEDICAL' });

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (currentUser) => {
      setUser(currentUser);
      setLoadingAuth(false);
    });
    return () => unsubscribe();
  }, []);

  useEffect(() => {
    if (!user) return;
    const uid = user.uid;

    const unsubProviders = onSnapshot(collection(db, 'users', uid, 'care_providers'), (snap) => {
      setCareProviders(snap.docs.map(d => ({ id: d.id, ...d.data() })));
    });

    const unsubTasks = onSnapshot(collection(db, 'users', uid, 'tasks'), (snap) => {
      setTasks(snap.docs.map(d => ({ id: d.id, ...d.data() })));
    });

    const unsubApts = onSnapshot(collection(db, 'users', uid, 'appointments'), (snap) => {
      setAppointments(snap.docs.map(d => ({ id: d.id, ...d.data() })));
    });

    const unsubExpenses = onSnapshot(collection(db, 'users', uid, 'expenses'), (snap) => {
      setExpenses(snap.docs.map(d => ({ id: d.id, ...d.data() })));
    });

    const unsubDocs = onSnapshot(collection(db, 'users', uid, 'documents'), (snap) => {
      setDocuments(snap.docs.map(d => ({ id: d.id, ...d.data() })));
    });

    return () => {
      unsubProviders();
      unsubTasks();
      unsubApts();
      unsubExpenses();
      unsubDocs();
    };
  }, [user]);

  const handleGoogleLogin = async () => {
    try {
      await signInWithPopup(auth, googleProvider);
    } catch (err) {
      console.error("Login error:", err);
      alert("Échec de connexion Google: " + err.message);
    }
  };

  const handleSaveContact = async (e) => {
    e.preventDefault();
    if (!user || !newContact.name || !newContact.phone) return;
    try {
      await addDoc(collection(db, 'users', user.uid, 'care_providers'), {
        ...newContact,
        createdAt: new Date(),
        updatedAt: new Date()
      });
      setNewContact({ name: '', providerType: 'Médecin Référent', affiliation: 'RMA of New York', phone: '', email: '', address: '', notes: '', isEmergency: false, category: 'MEDICAL' });
      setShowAddContact(false);
    } catch (err) {
      console.error("Save contact error:", err);
      alert("Erreur d'enregistrement : " + err.message);
    }
  };

  const handleDeleteContact = async (id) => {
    if (!user || !confirm("Supprimer ce contact ?")) return;
    await deleteDoc(doc(db, 'users', user.uid, 'care_providers', id));
  };

  if (loadingAuth) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-amber-50">
        <div className="text-amber-800 font-medium text-lg animate-pulse">Chargement de BF Leather Web Hub...</div>
      </div>
    );
  }

  if (!user) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-amber-50 via-orange-50 to-stone-100 p-4">
        <div className="max-w-md w-full bg-white rounded-3xl shadow-xl p-8 border border-amber-100 text-center">
          <div className="w-20 h-20 bg-amber-600 rounded-2xl mx-auto flex items-center justify-center text-white text-3xl font-bold shadow-lg mb-6">
            BF
          </div>
          <h1 className="text-2xl font-black text-stone-900 mb-2">BF Leather PMA & Grossesse</h1>
          <p className="text-stone-600 text-sm mb-8">
            Connectez-vous avec votre compte Google autorisé pour synchroniser en temps réel vos protocoles, rendez-vous et répertoire médical avec l'application Android.
          </p>
          <button
            onClick={handleGoogleLogin}
            className="w-full py-4 px-6 bg-stone-900 hover:bg-stone-800 text-white font-bold rounded-2xl shadow-md transition flex items-center justify-center space-x-3"
          >
            <svg className="w-5 h-5" viewBox="0 0 24 24">
              <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
              <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
              <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"/>
              <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"/>
            </svg>
            <span>Connexion avec Google</span>
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-stone-50 flex flex-col">
      {/* Header */}
      <header className="bg-white border-b border-stone-200 sticky top-0 z-30 shadow-xs">
        <div className="max-w-7xl mx-auto px-4 h-16 flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 bg-amber-600 rounded-xl flex items-center justify-center text-white font-bold text-lg shadow-sm">
              BF
            </div>
            <div>
              <h1 className="font-bold text-stone-900 leading-tight">BF Leather Web</h1>
              <p className="text-xs text-stone-500">RMA New York • Fatou & Partenaire</p>
            </div>
          </div>

          <div className="flex items-center space-x-4">
            <div className="text-right hidden sm:block">
              <div className="text-xs font-semibold text-stone-800">{user.email}</div>
              <div className="text-[10px] text-emerald-600 font-medium">● Synchronisé en direct</div>
            </div>
            <button
              onClick={() => signOut(auth)}
              className="p-2 text-stone-500 hover:text-red-600 hover:bg-stone-100 rounded-xl transition"
              title="Déconnexion"
            >
              <LogOut className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Navigation Tabs */}
        <div className="max-w-7xl mx-auto px-4 flex space-x-2 border-t border-stone-100 overflow-x-auto">
          {[
            { id: 'dashboard', label: 'Tableau de Bord', icon: Heart },
            { id: 'phonebook', label: 'Répertoire Médical', icon: Phone },
            { id: 'tasks', label: 'Tâches & Protocole', icon: CheckCircle2 },
            { id: 'appointments', label: 'Rendez-vous', icon: Calendar },
            { id: 'expenses', label: 'Budget & Dépenses', icon: DollarSign },
            { id: 'vault', label: 'Coffre & Documents', icon: Shield },
          ].map((tab) => {
            const Icon = tab.icon;
            const active = currentTab === tab.id;
            return (
              <button
                key={tab.id}
                onClick={() => setCurrentTab(tab.id)}
                className={`flex items-center space-x-2 py-3 px-4 text-sm font-semibold border-b-2 whitespace-nowrap transition ${
                  active
                    ? 'border-amber-600 text-amber-700 bg-amber-50/50'
                    : 'border-transparent text-stone-600 hover:text-stone-900 hover:bg-stone-50'
                }`}
              >
                <Icon className="w-4 h-4" />
                <span>{tab.label}</span>
              </button>
            );
          })}
        </div>
      </header>

      {/* Main Content Area */}
      <main className="max-w-7xl mx-auto px-4 py-8 flex-1 w-full">
        {currentTab === 'dashboard' && (
          <div className="space-y-6">
            <div className="bg-gradient-to-r from-amber-600 to-orange-600 rounded-3xl p-6 md:p-8 text-white shadow-lg">
              <div className="flex items-center space-x-2 text-amber-200 text-xs font-bold uppercase tracking-wider mb-2">
                <Sparkles className="w-4 h-4" />
                <span>Parcours PMA & Maternité 2026-2027</span>
              </div>
              <h2 className="text-2xl md:text-3xl font-black mb-2">Bienvenue sur votre portail synchronisé</h2>
              <p className="text-amber-100 text-sm max-w-2xl mb-6">
                Toutes vos données de prise de sang, protocoles Estrace, Progestérone IM, et carnets d'adresses RMA of New York sont connectées en temps réel avec votre application mobile Android.
              </p>
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                <div className="bg-white/10 backdrop-blur-md rounded-2xl p-4 border border-white/20">
                  <div className="text-2xl font-black">{careProviders.length}</div>
                  <div className="text-xs text-amber-100">Contacts Médicaux</div>
                </div>
                <div className="bg-white/10 backdrop-blur-md rounded-2xl p-4 border border-white/20">
                  <div className="text-2xl font-black">{tasks.length}</div>
                  <div className="text-xs text-amber-100">Tâches & Étapes</div>
                </div>
                <div className="bg-white/10 backdrop-blur-md rounded-2xl p-4 border border-white/20">
                  <div className="text-2xl font-black">{appointments.length}</div>
                  <div className="text-xs text-amber-100">Rendez-vous Clés</div>
                </div>
                <div className="bg-white/10 backdrop-blur-md rounded-2xl p-4 border border-white/20">
                  <div className="text-2xl font-black">{expenses.length}</div>
                  <div className="text-xs text-amber-100">Dépenses Suivies</div>
                </div>
              </div>
            </div>

            <div className="grid md:grid-cols-2 gap-6">
              <div className="bg-white rounded-3xl p-6 shadow-sm border border-stone-200">
                <h3 className="font-bold text-lg text-stone-900 mb-4 flex items-center justify-between">
                  <span>Urgences & Contacts Clés</span>
                  <button onClick={() => setCurrentTab('phonebook')} className="text-xs text-amber-600 font-bold hover:underline">Voir tout →</button>
                </h3>
                <div className="space-y-3">
                  {careProviders.slice(0, 3).map(p => (
                    <div key={p.id} className="p-3 bg-stone-50 rounded-2xl border border-stone-100 flex items-center justify-between">
                      <div>
                        <div className="font-bold text-sm text-stone-900">{p.name}</div>
                        <div className="text-xs text-stone-500">{p.providerType} • {p.phone}</div>
                      </div>
                      <a href={`tel:${p.phone}`} className="p-2 bg-amber-600 text-white rounded-xl shadow-xs hover:bg-amber-700">
                        <Phone className="w-4 h-4" />
                      </a>
                    </div>
                  ))}
                </div>
              </div>

              <div className="bg-white rounded-3xl p-6 shadow-sm border border-stone-200">
                <h3 className="font-bold text-lg text-stone-900 mb-4 flex items-center justify-between">
                  <span>Prochaines Étapes PMA</span>
                  <button onClick={() => setCurrentTab('tasks')} className="text-xs text-amber-600 font-bold hover:underline">Voir tout →</button>
                </h3>
                <div className="space-y-3">
                  {tasks.slice(0, 3).map(t => (
                    <div key={t.id} className="p-3 bg-stone-50 rounded-2xl border border-stone-100">
                      <div className="font-bold text-sm text-stone-900">{t.title}</div>
                      <div className="text-xs text-stone-500">{t.date} • {t.category}</div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        )}

        {currentTab === 'phonebook' && (
          <div className="space-y-6">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-2xl font-black text-stone-900">Répertoire & Urgences Médicales</h2>
                <p className="text-stone-500 text-sm">Gérez l'équipe RMA of New York et vos contacts d'urgence.</p>
              </div>
              <button
                onClick={() => setShowAddContact(true)}
                className="py-2.5 px-5 bg-amber-600 hover:bg-amber-700 text-white font-bold rounded-xl shadow-md flex items-center space-x-2 transition"
              >
                <Plus className="w-4 h-4" />
                <span>Nouveau Contact</span>
              </button>
            </div>

            <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-4">
              {careProviders.map(p => (
                <div key={p.id} className="bg-white rounded-2xl p-5 border border-stone-200 shadow-xs flex flex-col justify-between">
                  <div>
                    <div className="flex items-start justify-between mb-3">
                      <div>
                        <span className="text-xs font-bold px-2.5 py-1 bg-amber-50 text-amber-700 rounded-lg">{p.providerType}</span>
                        <h3 className="font-bold text-base text-stone-900 mt-1">{p.name}</h3>
                        <p className="text-xs text-stone-500">{p.affiliation}</p>
                      </div>
                      <button onClick={() => handleDeleteContact(p.id)} className="text-stone-400 hover:text-red-600 p-1">
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>

                    <div className="space-y-2 text-xs text-stone-600 my-4">
                      <div className="flex items-center space-x-2">
                        <Phone className="w-3.5 h-3.5 text-amber-600" />
                        <span className="font-semibold">{p.phone}</span>
                      </div>
                      {p.email && (
                        <div className="flex items-center space-x-2">
                          <Mail className="w-3.5 h-3.5 text-stone-400" />
                          <span>{p.email}</span>
                        </div>
                      )}
                      {p.address && (
                        <div className="flex items-center space-x-2">
                          <MapPin className="w-3.5 h-3.5 text-stone-400" />
                          <span className="truncate">{p.address}</span>
                        </div>
                      )}
                    </div>
                  </div>

                  <div className="pt-3 border-t border-stone-100 flex space-x-2">
                    <a href={`tel:${p.phone}`} className="flex-1 py-2 bg-amber-600 hover:bg-amber-700 text-white text-xs font-bold rounded-xl text-center shadow-xs flex items-center justify-center space-x-1">
                      <Phone className="w-3.5 h-3.5" />
                      <span>Appeler</span>
                    </a>
                    {p.email && (
                      <a href={`mailto:${p.email}`} className="py-2 px-3 bg-stone-100 hover:bg-stone-200 text-stone-700 text-xs font-bold rounded-xl text-center">
                        Email
                      </a>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {currentTab === 'tasks' && (
          <div className="space-y-6">
            <h2 className="text-2xl font-black text-stone-900">Tâches & Étapes du Protocole</h2>
            <div className="bg-white rounded-3xl p-6 border border-stone-200 shadow-sm space-y-4">
              {tasks.map(t => (
                <div key={t.id} className="p-4 bg-stone-50 rounded-2xl border border-stone-200 flex items-center justify-between">
                  <div>
                    <span className="text-[10px] font-bold px-2 py-0.5 bg-amber-100 text-amber-800 rounded-md">{t.category}</span>
                    <h3 className="font-bold text-stone-900 text-base mt-1">{t.title}</h3>
                    <p className="text-xs text-stone-500">{t.date} {t.details ? `• ${t.details}` : ''}</p>
                  </div>
                  <div className="text-xs font-bold text-stone-500">
                    {t.isCompleted ? '✓ Terminé' : 'En cours'}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {currentTab === 'appointments' && (
          <div className="space-y-6">
            <h2 className="text-2xl font-black text-stone-900">Rendez-vous & Échographies</h2>
            <div className="grid md:grid-cols-2 gap-4">
              {appointments.map(a => (
                <div key={a.id} className="bg-white rounded-2xl p-5 border border-stone-200 shadow-xs">
                  <div className="flex justify-between items-start mb-2">
                    <span className="text-xs font-bold px-2.5 py-1 bg-amber-50 text-amber-700 rounded-lg">{a.dateStr} @ {a.timeStr}</span>
                    <span className="text-xs text-stone-500 font-semibold">{a.doctor}</span>
                  </div>
                  <h3 className="font-bold text-stone-900 text-base">{a.title}</h3>
                  <p className="text-xs text-stone-500 mt-1">{a.location}</p>
                  {a.clinicalNotes && <p className="text-xs text-stone-600 mt-3 p-3 bg-stone-50 rounded-xl">{a.clinicalNotes}</p>}
                </div>
              ))}
            </div>
          </div>
        )}

        {currentTab === 'expenses' && (
          <div className="space-y-6">
            <h2 className="text-2xl font-black text-stone-900">Budget & Suivi des Dépenses FIV</h2>
            <div className="bg-white rounded-3xl p-6 border border-stone-200 shadow-sm space-y-4">
              {expenses.map(ex => (
                <div key={ex.id} className="p-4 bg-stone-50 rounded-2xl border border-stone-200 flex items-center justify-between">
                  <div>
                    <span className="text-[10px] font-bold px-2 py-0.5 bg-emerald-100 text-emerald-800 rounded-md">{ex.category}</span>
                    <h3 className="font-bold text-stone-900 text-base mt-1">{ex.title}</h3>
                    <p className="text-xs text-stone-500">{ex.dateStr} • {ex.paymentStatus}</p>
                  </div>
                  <div className="text-right">
                    <div className="text-lg font-black text-stone-900">${ex.totalBill?.toFixed(2)}</div>
                    <div className="text-[10px] text-stone-500">{ex.currency}</div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {currentTab === 'vault' && (
          <div className="space-y-6">
            <h2 className="text-2xl font-black text-stone-900">Coffre & Documents Médicaux</h2>
            <div className="grid md:grid-cols-2 gap-4">
              {documents.map(d => (
                <div key={d.id} className="bg-white rounded-2xl p-5 border border-stone-200 shadow-xs">
                  <span className="text-xs font-bold px-2 py-0.5 bg-blue-50 text-blue-700 rounded-md">{d.category}</span>
                  <h3 className="font-bold text-stone-900 text-base mt-2">{d.title}</h3>
                  <p className="text-xs text-stone-600 mt-2">{d.summary}</p>
                  <div className="text-[10px] text-stone-400 mt-4">{d.dateStr}</div>
                </div>
              ))}
            </div>
          </div>
        )}
      </main>

      {/* Add Contact Modal */}
      {showAddContact && (
        <div className="fixed inset-0 bg-stone-900/60 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-3xl max-w-lg w-full p-6 shadow-2xl border border-stone-200">
            <h3 className="text-xl font-black text-stone-900 mb-4">Ajouter un Contact Médical</h3>
            <form onSubmit={handleSaveContact} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-stone-700 mb-1">Nom du médecin ou établissement *</label>
                <input
                  type="text"
                  required
                  value={newContact.name}
                  onChange={e => setNewContact({...newContact, name: e.target.value})}
                  placeholder="ex: Dr. Anate Brauer"
                  className="w-full px-4 py-2.5 rounded-xl border border-stone-300 text-sm focus:outline-none focus:ring-2 focus:ring-amber-600"
                />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-stone-700 mb-1">Rôle / Spécialité</label>
                  <input
                    type="text"
                    value={newContact.providerType}
                    onChange={e => setNewContact({...newContact, providerType: e.target.value})}
                    className="w-full px-4 py-2.5 rounded-xl border border-stone-300 text-sm focus:outline-none focus:ring-2 focus:ring-amber-600"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-stone-700 mb-1">Téléphone *</label>
                  <input
                    type="text"
                    required
                    value={newContact.phone}
                    onChange={e => setNewContact({...newContact, phone: e.target.value})}
                    placeholder="212-756-5777"
                    className="w-full px-4 py-2.5 rounded-xl border border-stone-300 text-sm focus:outline-none focus:ring-2 focus:ring-amber-600"
                  />
                </div>
              </div>
              <div>
                <label className="block text-xs font-bold text-stone-700 mb-1">Affiliation / Clinique</label>
                <input
                  type="text"
                  value={newContact.affiliation}
                  onChange={e => setNewContact({...newContact, affiliation: e.target.value})}
                  className="w-full px-4 py-2.5 rounded-xl border border-stone-300 text-sm focus:outline-none focus:ring-2 focus:ring-amber-600"
                />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-stone-700 mb-1">Email</label>
                  <input
                    type="email"
                    value={newContact.email}
                    onChange={e => setNewContact({...newContact, email: e.target.value})}
                    className="w-full px-4 py-2.5 rounded-xl border border-stone-300 text-sm focus:outline-none focus:ring-2 focus:ring-amber-600"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-stone-700 mb-1">Catégorie</label>
                  <select
                    value={newContact.category}
                    onChange={e => setNewContact({...newContact, category: e.target.value})}
                    className="w-full px-4 py-2.5 rounded-xl border border-stone-300 text-sm focus:outline-none focus:ring-2 focus:ring-amber-600"
                  >
                    <option value="MEDICAL">Médical</option>
                    <option value="CLINIC">Clinique</option>
                    <option value="PHARMACY">Pharmacie</option>
                    <option value="FAMILY">Famille</option>
                    <option value="INSURANCE">Assurance</option>
                  </select>
                </div>
              </div>
              <div>
                <label className="block text-xs font-bold text-stone-700 mb-1">Adresse</label>
                <input
                  type="text"
                  value={newContact.address}
                  onChange={e => setNewContact({...newContact, address: e.target.value})}
                  className="w-full px-4 py-2.5 rounded-xl border border-stone-300 text-sm focus:outline-none focus:ring-2 focus:ring-amber-600"
                />
              </div>

              <div className="flex items-center space-x-2 pt-2">
                <input
                  type="checkbox"
                  id="isEmergency"
                  checked={newContact.isEmergency}
                  onChange={e => setNewContact({...newContact, isEmergency: e.target.checked})}
                  className="w-4 h-4 text-amber-600 rounded border-stone-300 focus:ring-amber-500"
                />
                <label htmlFor="isEmergency" className="text-xs font-bold text-stone-700">Contact d'urgence prioritaire 24/7</label>
              </div>

              <div className="flex space-x-3 pt-4">
                <button
                  type="button"
                  onClick={() => setShowAddContact(false)}
                  className="flex-1 py-3 bg-stone-100 hover:bg-stone-200 text-stone-700 text-sm font-bold rounded-xl transition"
                >
                  Annuler
                </button>
                <button
                  type="submit"
                  className="flex-1 py-3 bg-amber-600 hover:bg-amber-700 text-white text-sm font-bold rounded-xl shadow-md transition"
                >
                  Enregistrer
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
