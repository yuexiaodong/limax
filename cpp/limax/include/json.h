#pragma once

namespace limax {
	namespace json_impl {
		struct JSONException
		{
			std::string message;
			JSONException(int32_t _line)
				: message("JSONException __LINE__ = " + std::to_string(_line))
			{}
		};
#define THROW_JSON_EXCEPTION throw JSONException(__LINE__)
		class JSONBuilder;
		class JSONMarshal
		{
		public:
			virtual ~JSONMarshal() {}
			virtual JSONBuilder& marshal(JSONBuilder &jb) const = 0;
		};
#pragma region class JSONBuilder
		class JSONBuilder
		{
			template<typename T> friend class JSON;
			std::string sb;
			void _append(wchar_t c)
			{
				switch (c)
				{
				case '"':
					sb.append("\\\"");
					break;
				case '\\':
					sb.append("\\\\");
					break;
				case '\b':
					sb.append("\\b");
					break;
				case '\f':
					sb.append("\\f");
					break;
				case '\n':
					sb.append("\\n");
					break;
				case '\r':
					sb.append("\\r");
					break;
				case '\t':
					sb.append("\\t");
					break;
				default:
					if (c < 256)
						sb.push_back((char)c);
					else
					{
						auto hex = [](char c){return c < 10 ? c + '0' : c + 'a' - 10; };
						sb.push_back('\\');
						sb.push_back('u');
						sb.push_back(hex((c >> 12) & 15));
						sb.push_back(hex((c >> 8) & 15));
						sb.push_back(hex((c >> 4) & 15));
						sb.push_back(hex(c & 15));
					}
				}
			}
			void _append(int8_t v)
			{
				sb.append(std::to_string(v));
			}
			void _append(uint8_t v)
			{
				sb.append(std::to_string(v));
			}
			void _append(int16_t v)
			{
				sb.append(std::to_string(v));
			}
			void _append(uint16_t v)
			{
				sb.append(std::to_string(v));
			}
			void _append(int32_t v)
			{
				sb.append(std::to_string(v));
			}
			void _append(uint32_t v)
			{
				sb.append(std::to_string(v));
			}
			void _append(int64_t v)
			{
				sb.append(std::to_string(v));
			}
			void _append(uint64_t v)
			{
				sb.append(std::to_string(v));
			}
			void _append(float v)
			{
				sb.append(std::to_string(v));
			}
			void _append(double v)
			{
				sb.append(std::to_string(v));
			}
			void _append(bool v)
			{
				_append(v ? "true" : "false");
			}
			void _append(const std::string& v)
			{
				for (auto c : v)
					_append((wchar_t)c);
			}
			void _append(const std::wstring& v)
			{
				for (auto c : v)
					_append(c);
			}
			template<typename II>
			void _append(II it, II ie, char c0, char c1)
			{
				const char* comma = "";
				sb.push_back(c0);
				for (; it != ie; ++it)
				{
					sb.append(comma);
					append(*it);
					comma = ",";
				}
				sb.push_back(c1);
			}
			void _append(const char *p)
			{
				sb.append(p);
			}
		public:
			JSONBuilder& append(const JSONMarshal& v)
			{
				return v.marshal(*this);
			}
			JSONBuilder& append(int8_t v)
			{
				_append(v);
				return *this;
			}
			JSONBuilder& append(uint8_t v)
			{
				_append(v);
				return *this;
			}
			JSONBuilder& append(int16_t v)
			{
				_append(v);
				return *this;
			}
			JSONBuilder& append(uint16_t v)
			{
				_append(v);
				return *this;
			}
			JSONBuilder& append(int32_t v)
			{
				_append(v);
				return *this;
			}
			JSONBuilder& append(uint32_t v)
			{
				_append(v);
				return *this;
			}
			JSONBuilder& append(int64_t v)
			{
				_append(v);
				return *this;
			}
			JSONBuilder& append(uint64_t v)
			{
				_append(v);
				return *this;
			}
			JSONBuilder& append(float v)
			{
				_append(v);
				return *this;
			}
			JSONBuilder& append(double v)
			{
				_append(v);
				return *this;
			}
			JSONBuilder& append(bool v)
			{
				_append(v);
				return *this;
			}
			JSONBuilder& append(char v)
			{
				sb.push_back('"');
				_append((wchar_t)v);
				sb.push_back('"');
				return *this;
			}
			JSONBuilder& append(wchar_t v)
			{
				sb.push_back('"');
				_append(v);
				sb.push_back('"');
				return *this;
			}
			JSONBuilder& append(const std::string& v)
			{
				sb.push_back('"');
				_append(v);
				sb.push_back('"');
				return *this;
			}
			JSONBuilder& append(const std::wstring& v)
			{
				sb.push_back('"');
				_append(v);
				sb.push_back('"');
				return *this;
			}
			JSONBuilder& append(const char *v)
			{
				return append(std::string(v));
			}
			JSONBuilder& append(const wchar_t *v)
			{
				return append(std::wstring(v));
			}
			template<typename T, typename A>
			JSONBuilder& append(const std::vector<T, A>& v)
			{
				_append(v.begin(), v.end(), '[', ']');
				return *this;
			}
			template<typename T, typename A>
			JSONBuilder& append(const std::list<T, A>& v)
			{
				_append(v.begin(), v.end(), '[', ']');
				return *this;
			}
			template<typename T, typename A>
			JSONBuilder& append(const std::deque<T, A>& v)
			{
				_append(v.begin(), v.end(), '[', ']');
				return *this;
			}
			template<typename K, typename H, typename E, typename A>
			JSONBuilder& append(const std::unordered_set<K, H, E, A>& v)
			{
				_append(v.begin(), v.end(), '[', ']');
				return *this;
			}
			template<typename K, typename V, typename H, typename E, typename A>
			JSONBuilder& append(const std::unordered_map<K, V, H, E, A>& v)
			{
				_append(v.begin(), v.end(), '{', '}');
				return *this;
			}
			template<typename T1, typename T2>
			JSONBuilder& append(const std::pair<T1, T2>& v)
			{
				sb.push_back('"');
				_append(v.first);
				sb.append("\":");
				append(v.second);
				return *this;
			}
			JSONBuilder& begin()
			{
				sb.push_back('{');
				return *this;
			}
			JSONBuilder& end()
			{
				if (sb.back() == ',')
					sb.pop_back();
				sb.push_back('}');
				return *this;
			}
			JSONBuilder& comma()
			{
				sb.push_back(',');
				return *this;
			}
			JSONBuilder& colon()
			{
				sb.push_back(':');
				return *this;
			}
			JSONBuilder& null()
			{
				sb.append("null");
				return *this;
			}
			std::string toString() const
			{
				return sb;
			}
		};
#pragma endregion
#pragma region class _JSON
		template<class Char>
		class JSON : public JSONMarshal
		{
		private:
			struct _Object{
				virtual ~_Object(){}
			};
		public:
			typedef typename std::shared_ptr<_Object> Object;
			typedef typename std::basic_string<Char> string;
		private:
			struct _String : public _Object, public string
			{
				_String() {}
				_String(const char *p) : string(p) {}
				_String(const string& p) : string(p) {}
			};
		public:
			typedef typename std::shared_ptr<_String> String;
		private:
			struct _Number : public _String {};
		public:
			typedef typename std::shared_ptr<_Number> Number;
		private:
			struct StringHash
			{
				size_t operator()(const String& a) const { return std::hash<string>()(*a); }
			};
			struct StringEqual
			{
				bool operator()(const String& a, const String& b) const { return *a == *b; }
			};
			struct _Map : public _Object, public std::unordered_map < String, Object, StringHash, StringEqual >
			{
			};
		public:
			typedef typename std::shared_ptr<_Map> Map;
		private:
			class _List : public _Object, public std::vector <Object> {};
			Object data;
		public:
			typedef typename std::shared_ptr<_List> List;
		public:
			static const Object Undefined;
			static const Object Null;
			static const Object True;
			static const Object False;
			JSON(Object _data) : data(_data){}
		private:
			template<class R> static R cast(Object data)
			{
				return std::dynamic_pointer_cast<typename R::element_type>(data);
			}
			template<class R> R cast() const
			{
				if (R r = cast<R>(data))
					return r;
				THROW_JSON_EXCEPTION;
			}
			string make_string(const char *p) const
			{
				string s;
				size_t l = strlen(p);
				for (size_t i = 0; i < l; i++)
					s.push_back(p[i]);
				return s;
			}
			bool tryLong(string s, int64_t& r) const
			{
				try
				{
					size_t idx;
					r = std::stoll(s, &idx);
					if (idx == s.length())
						return true;
				}
				catch (...){}
				return false;
			}
			bool tryDouble(string s, double& r) const
			{
				try
				{
					size_t idx;
					r = std::stod(s, &idx);
					if (idx == s.length())
						return true;
				}
				catch (...){}
				return false;
			}
			static JSONBuilder& marshal(JSONBuilder &jb, Object data)
			{
				if (data == Undefined)
					THROW_JSON_EXCEPTION;
				else if (data == Null)
					jb._append("null");
				else if (data == True)
					jb._append("true");
				else if (data == False)
					jb._append("false");
				else if (Number n = cast<Number>(data))
					jb._append(*n);
				else if (String s = cast<String>(data))
					jb.append(*s);
				else if (List list = cast<List>(data))
				{
					const char *comma = "";
					jb._append("[");
					for (auto o : *list)
					{
						jb._append(comma);
						marshal(jb, o);
						comma = ",";
					}
					jb._append("]");
				}
				else if (Map map = cast<Map>(data))
				{
					const char *comma = "";
					jb._append("{");
					for (auto e : *map)
					{
						jb._append(comma);
						jb._append("\"");
						jb._append(*e.first);
						jb._append("\":");
						marshal(jb, e.second);
						comma = ",";
					}
					jb._append("}");
				}
				return jb;
			}
		public:
			std::shared_ptr<JSON<Char>> get(const string& key) const
			{
				Map map = cast<Map>();
				auto it = map->find(std::make_shared<typename String::element_type>(key));
				return std::shared_ptr<JSON<Char>>(new JSON<Char>(it == map->end() ? Undefined : (*it).second));
			}
			std::vector<string> keySet() const
			{
				Map map = cast<Map>();
				std::vector<string> r;
				for (auto e : *map)
					r.push_back(*e.first);
				return r;
			}
			std::shared_ptr<JSON<Char>> get(size_t index) const
			{
				List list = cast<List>();
				return std::shared_ptr<JSON<Char>>(new JSON<Char>(index >= list->size() ? Undefined : (*list)[index]));
			}
			std::vector<std::shared_ptr<JSON<Char>>> toArray() const
			{
				List list = cast<List>();
				std::vector<std::shared_ptr<JSON<Char>>> r;
				for (auto e : *list)
					r.push_back(std::shared_ptr<JSON<Char>>(new JSON<Char>(e)));
				return r;
			}
			string toString() const
			{
				if (data == Undefined)
					return make_string("undefined");
				if (data == Null)
					return make_string("null");
				if (data == True)
					return make_string("true");
				if (data == False)
					return make_string("false");
				if (std::dynamic_pointer_cast<typename Map::element_type>(data))
					return make_string("<Object>");
				if (std::dynamic_pointer_cast<typename List::element_type>(data))
					return make_string("<List>");
				return *std::dynamic_pointer_cast<typename String::element_type>(data);
			}
			bool booleanValue() const
			{
				if (data == True) return true;
				if (data == False || data == Null || data == Undefined) return false;
				try
				{
					string& s = *cast<String>();
					if (s.length() == 0)
						return false;
					int64_t lv;
					if (tryLong(s, lv))
						return lv != 0L;
					double dv;
					if (tryDouble(s, dv))
						return dv != 0.0;
				}
				catch (JSONException){}
				return true;
			}
			int32_t intValue() const
			{
				return (int32_t)doubleValue();
			}
			int64_t longValue() const
			{
				if (data == True) return 1L;
				if (data == False || data == Null) return 0L;
				string&s = *cast<String>();
				if (s.length() > 0)
				{
					int64_t lv;
					if (tryLong(s, lv))
						return lv;
					double dv;
					if (tryDouble(s, dv))
						return (int64_t)dv;
				}
				THROW_JSON_EXCEPTION;
			}
			double doubleValue() const
			{
				if (data == True) return 1;
				if (data == False || data == Null) return 0;
				string&s = *cast<String>();
				if (s.length() > 0)
				{
					double dv;
					if (tryDouble(s, dv))
						return dv;
				}
				THROW_JSON_EXCEPTION;
			}
			bool isUndefined() const
			{
				return data == Undefined;
			}
			bool isNull() const
			{
				return data == Null;
			}
			bool isBoolean() const
			{
				return data == True || data == False;
			}
			bool isString() const
			{
				try
				{
					cast<String>();
					return !isNumber();
				}
				catch (JSONException) {}
				return false;
			}
			bool isNumber() const
			{
				try
				{
					cast<Number>();
					return true;
				}
				catch (JSONException) {}
				return false;
			}
			bool isObject() const
			{
				try
				{
					cast<Map>();
					return true;
				}
				catch (JSONException) {}
				return false;
			}
			bool isArray() const
			{
				try
				{
					cast<List>();
					return true;
				}
				catch (JSONException) {}
				return false;
			}
			JSONBuilder& marshal(JSONBuilder &jb) const
			{
				return marshal(jb, data);
			}
			static std::shared_ptr<JSON<Char>> parse(const string& text);
			template<typename T>
			static std::string stringify(const T& obj)
			{
				return JSONBuilder().append(obj).toString();
			}
			static std::string stringify(const char* obj)
			{
				return JSONBuilder().append(std::string(obj)).toString();
			}
			static std::string stringify(const wchar_t* obj)
			{
				return JSONBuilder().append(std::wstring(obj)).toString();
			}
			static std::string stringify(std::shared_ptr<JSON<char>> obj)
			{
				return stringify(*obj);
			}
			static std::string stringify(std::shared_ptr<JSON<wchar_t>> obj)
			{
				return stringify(*obj);
			}
		};
		template<class Char> const typename JSON<Char>::Object JSON<Char>::Undefined(new JSON<Char>::_Object());
		template<class Char> const typename JSON<Char>::Object JSON<Char>::Null(new JSON<Char>::_Object());
		template<class Char> const typename JSON<Char>::Object JSON<Char>::True(new JSON<Char>::_Object());
		template<class Char> const typename JSON<Char>::Object JSON<Char>::False(new JSON<Char>::_Object());
#pragma endregion
#pragma region class JSONDecoder
		template <class Char>
		class JSONDecoder
		{
			template<class T> friend class JSON;
		public:
			typedef typename std::function<void(std::shared_ptr<JSON<Char>>)> Consumer;
		private:
			typedef typename JSON<Char>::Object Object;
			typedef typename JSON<Char>::Map Map;
			typedef typename JSON<Char>::List List;
			typedef typename JSON<Char>::String String;
			typedef typename JSON<Char>::Number Number;
			struct _JSONValue
			{
				virtual ~_JSONValue() {}
				virtual bool accept(Char c) = 0;
				virtual void reduce(Object v) {}
			};
			Consumer consumer;
			struct _JSONRoot : public _JSONValue
			{
				JSONDecoder &decoder;
				_JSONRoot(JSONDecoder& _decoder) : decoder(_decoder) {}
				bool accept(Char c)
				{
					if (iswspace(c))
						return true;
					if (decoder.json)
						THROW_JSON_EXCEPTION;
					return false;
				}
				void reduce(Object v)
				{
					if (decoder.consumer)
						decoder.consumer(std::make_shared<JSON<Char>>(v));
					else
						decoder.json = std::make_shared<JSON<Char>>(v);
				}
			};
			typedef typename std::shared_ptr<_JSONRoot> JSONRoot;
			typedef typename std::shared_ptr<_JSONValue> JSONValue;
			JSONRoot root;
			JSONValue current;
			JSONValue change;
			std::shared_ptr<JSON<Char>> json;
		public:
			JSONDecoder(Consumer _consumer) : consumer(_consumer), root(std::shared_ptr<_JSONRoot>(new _JSONRoot(*this))), current(root)
			{
			}

			JSONDecoder() : root(std::shared_ptr<_JSONRoot>(new _JSONRoot(*this))), current(root)
			{
			}
		private:
			struct _JSONObject : public _JSONValue
			{
				JSONDecoder &decoder;
				JSONValue parent;
				Map map;
				String key;
				int stage = 0;
				_JSONObject(JSONDecoder& _decoder) : decoder(_decoder), parent(_decoder.current), map(std::make_shared<typename Map::element_type>()) {}
				bool accept(Char c)
				{
					switch (stage)
					{
					case 0:
						stage = 1;
						return true;
					case 1:
						if (iswspace(c))
							return true;
						if (c == '}')
						{
							(decoder.change = parent)->reduce(map);
							return true;
						}
						return false;
					case 2:
						if (iswspace(c))
							return true;
						if (c == ':' || c == '=')
						{
							stage = 3;
							return true;
						}
						THROW_JSON_EXCEPTION;
					case 4:
						if (iswspace(c))
							return true;
						if (c == ',' || c == ';')
						{
							stage = 1;
							return true;
						}
						if (c == '}')
						{
							(decoder.change = parent)->reduce(map);
							return true;
						}
						THROW_JSON_EXCEPTION;
					}
					return iswspace(c) ? true : false;
				}
				void reduce(Object v) {
					if (stage == 1) {
						key = std::dynamic_pointer_cast<typename String::element_type>(v);
						if (!key)
							THROW_JSON_EXCEPTION;
						stage = 2;
					}
					else {
						map->insert(std::make_pair(key, v));
						stage = 4;
					}
				}
			};
			struct _JSONArray : public _JSONValue
			{
				JSONDecoder &decoder;
				JSONValue parent;
				List list;
				int stage = 0;
				_JSONArray(JSONDecoder& _decoder) : decoder(_decoder), parent(_decoder.current), list(std::make_shared<typename List::element_type>()){}
				bool accept(Char c)
				{
					switch (stage)
					{
					case 0:
						stage = 1;
						return true;
					case 1:
						if (iswspace(c))
							return true;
						if (c == ']')
						{
							(decoder.change = parent)->reduce(list);
							return true;
						}
						return false;
					default:
						if (iswspace(c))
							return true;
						if (c == ',' || c == ';')
						{
							stage = 1;
							return true;
						}
						if (c == ']')
						{
							(decoder.change = parent)->reduce(list);
							return true;
						}
						THROW_JSON_EXCEPTION;
					}
				}
				void reduce(Object v)
				{
					list->push_back(v);
					stage = 2;
				}
			};
			struct _JSONString : public _JSONValue
			{
				JSONDecoder &decoder;
				JSONValue parent;
				String sb;
				int stage = 0;
				_JSONString(JSONDecoder& _decoder) : decoder(_decoder), parent(_decoder.current), sb(std::make_shared<typename String::element_type>()){}
				static int hex(Char c)
				{
					if (c >= '0' && c <= '9')
						return c - '0';
					if (c >= 'A' && c <= 'F')
						return c - 'A' + 10;
					if (c >= 'a' && c <= 'f')
						return c - 'a' + 10;
					THROW_JSON_EXCEPTION;
				}
				bool accept(Char c)
				{
					if (stage < 0)
					{
						stage = (stage << 4) | hex(c);
						if ((stage & 0xffff0000) == 0xfff00000)
						{
							sb->push_back((Char)stage);
							stage = 0x40000000;
						}
					}
					else if ((stage & 0x20000000) != 0)
					{
						switch (c)
						{
						case '"':
							sb->push_back('"');
							break;
						case '\\':
							sb->push_back('\\');
							break;
						case 'b':
							sb->push_back('\b');
							break;
						case 'f':
							sb->push_back('\f');
							break;
						case 'n':
							sb->push_back('\n');
							break;
						case 'r':
							sb->push_back('\r');
							break;
						case 't':
							sb->push_back('\t');
							break;
						case 'u':
							stage = -16;
							break;
						}
						stage &= ~0x20000000;
					}
					else if (c == '"')
					{
						if ((stage & 0x40000000) != 0)
							(decoder.change = parent)->reduce(sb);
						stage |= 0x40000000;
					}
					else if (c == '\\')
						stage |= 0x20000000;
					else
						sb->push_back(c);
					return true;
				}
			};
			struct _JSONNumber : public _JSONValue
			{
				JSONDecoder &decoder;
				JSONValue parent;
				Number sb;
				_JSONNumber(JSONDecoder& _decoder) : decoder(_decoder), parent(_decoder.current), sb(std::make_shared<typename Number::element_type>()){}
				bool accept(Char c)
				{
					switch (c)
					{
					case '+':
					case '-':
					case '0':
					case '1':
					case '2':
					case '3':
					case '4':
					case '5':
					case '6':
					case '7':
					case '8':
					case '9':
					case 'E':
					case 'e':
					case '.':
						sb->push_back(c);
						return true;
					}
					size_t idx;
					try{ std::stod(*sb, &idx); }
					catch (...){ THROW_JSON_EXCEPTION; }
					if (idx != sb->length())
						THROW_JSON_EXCEPTION;
					(decoder.change = parent)->reduce(sb);
					return parent->accept(c);
				}
			};
			struct _JSONConst : public _JSONValue
			{
				JSONDecoder& decoder;
				JSONValue parent;
				std::vector<Char> match;
				Object value;
				int stage = 0;
				_JSONConst(JSONDecoder& _decoder, std::initializer_list<Char> _match, Object _value) : decoder(_decoder), parent(_decoder.current), match(_match), value(_value){ }
				bool accept(Char c)
				{
					if ((Char)towlower(c) != match[stage++])
						THROW_JSON_EXCEPTION;
					if ((size_t)stage == match.size())
						(decoder.change = parent)->reduce(value);
					return true;
				}
			};
			typedef typename std::shared_ptr<_JSONObject> JSONObject;
			typedef typename std::shared_ptr<_JSONArray> JSONArray;
			typedef typename std::shared_ptr<_JSONString> JSONString;
			typedef typename std::shared_ptr<_JSONNumber> JSONNumber;
			typedef typename std::shared_ptr<_JSONConst> JSONConst;
		public:
			void accept(Char c)
			{
				while (true)
				{
					bool accept = current->accept(c);
					if (change)
					{
						current = change;
						change.reset();
					}
					if (accept)
						break;
					switch (c)
					{
					case '{':
						current = std::make_shared<typename JSONObject::element_type>(*this);
						break;
					case '[':
						current = std::make_shared<typename JSONArray::element_type>(*this);
						break;
					case '"':
						current = std::make_shared<typename JSONString::element_type>(*this);
						break;
					case '-':
					case '0':
					case '1':
					case '2':
					case '3':
					case '4':
					case '5':
					case '6':
					case '7':
					case '8':
					case '9':
						current = std::make_shared<typename JSONNumber::element_type>(*this);
						break;
					case 't':
					case 'T':
						current = std::shared_ptr<_JSONConst>(new _JSONConst(*this, { 't', 'r', 'u', 'e' }, JSON<Char>::True));
						break;
					case 'f':
					case 'F':
						current = std::shared_ptr<_JSONConst>(new _JSONConst(*this, { 'f', 'a', 'l', 's', 'e' }, JSON<Char>::False));
						break;
					case 'n':
					case 'N':
						current = std::shared_ptr<_JSONConst>(new _JSONConst(*this, { 'n', 'u', 'l', 'l' }, JSON<Char>::Null));
						break;
					default:
						THROW_JSON_EXCEPTION;
					}
				}
			}
		private:
			void flush()
			{
				accept(' ');
			}
		public:
			std::shared_ptr<JSON<Char>> get()
			{
				return json;
			}
		};
#pragma endregion
		template<class Char> inline std::shared_ptr<JSON<Char>> JSON<Char>::parse(const string& text)
		{
			JSONDecoder<Char> decoder;
			for (auto c : text)
				decoder.accept(c);
			decoder.flush();
			return decoder.get();
		}
#undef THROW_JSON_EXCEPTION
	}
	typedef json_impl::JSONException JSONException;
	typedef json_impl::JSONBuilder JSONBuilder;
	typedef json_impl::JSONMarshal JSONMarshal;
	typedef json_impl::JSONDecoder<char> JSONDecoder;
	typedef json_impl::JSONDecoder<wchar_t> WJSONDecoder;
	typedef JSONDecoder::Consumer JSONConsumer;
	typedef WJSONDecoder::Consumer WJSONConsumer;
	typedef json_impl::JSON<char> JSON;
	typedef json_impl::JSON<wchar_t> WJSON;
} // namespace limax {
