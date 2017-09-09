local null = {}

local function parse(s)
  local p, l = 1, s:len()
  local c
  local C, T, O, A, S, N, V
  C = function ()
    if (p > l) then
      error("insufficient input string")
    end
    c = s:sub(p, p)
    p = p + 1
  end
  T = function ()
    while true do
      C()
      if c == ' ' or c == '\f' or c == '\n' or c == '\r' or c == '\t' or c == '\v' then
      else
        return
      end
    end
  end
  O = function ()
    local r = {}
    while true do
      local k
      while true do
        T()
        if c == '"' then
          k = S()
          break
        elseif c == '}' then
          return r
        elseif c == ',' or c == ';' then
        else
          error("want string or '}'")
        end
      end
      while true do
        T()
        if c == ':' or c == '=' then
          r[k] = V()
          break
        else
          error("want [:=]")
        end
      end
    end
  end
  A = function()
    local r = {}
    while true do
      T()
      if c == ']' then
        return r
      elseif c == ',' or c == ';' then
      else
        p = p - 1
        r[#r + 1] = V()
      end
    end
  end
  S = function()
    local m =""
    while true do
      C()
      if c == '"' then
        return m
      end
      if c == '\\' then
        C()
        if c == '"' then
        elseif c == '\\' then
          c = '\\'
        elseif c == 'b' then
          c = '\b'
        elseif c == 'f' then
          c = '\f'
        elseif c == 'n' then
          c = '\n'
        elseif c == 'r' then
          c = '\r'
        elseif c == 't' then
          c = '\t'
        else
          error("unsupported escape character " + c)
        end
      end
      m = m .. c
    end
  end
  N = function()
    local m = ""
    while true do
      C()
      if c >= '0' and c <= '9' or c == '.' or c == '-' or c == '+' or c == 'e' or c == 'E' then
        m = m .. c
      else
        p = p - 1
        return tonumber(m)
      end
    end
  end
  V = function()
    while p <= l do
      T()
      if c == '{' then
        return O()
      elseif c == '[' then
        return A()
      elseif c == '"' then
        return S()
      elseif c >= '0' and c <= '9' or c == '-' then
        p = p - 1
        return N()
      elseif c == 't' or c == 'T' then
        if s:sub(p, p + 2):lower() == "rue" then
          p = p + 3
          return true
        else
          error("true")
        end
      elseif c == 'f' or c == 'F' then
        if s:sub(p, p + 3):lower() == "alse" then
          p = p + 4
          return false
        else
          error("false")
        end
      elseif c == 'n' or c == 'N' then
        if s:sub(p, p + 2):lower() == "ull" then
          p = p + 3
          return null
        else
          error("true")
        end
      else
        error(c)
      end
    end
  end
  local r = V()
  while p <= l do
    c = s:sub(p, p)
    if c == ' ' or c == '\f' or c == '\n' or c == '\r' or c == '\t' or c == '\v' then
    else
      error("unexpected token " .. c)
    end
    p = p + 1
  end
  return r
end

local function stringify(o)
  local l = {}
  local function impl(o)
    if type(o) == "table" then
      if o == null then
        return "null"
      end
      for i = 1, #l do
        if o == l[i] then
          error("loop detected")
        end
      end
      table.insert(l, o)
      local s, e
      local a = {}
      if #o > 0 then
        s, e = '[', ']'
        for i = 1, #o do
          table.insert(a, impl(o[i]))
        end
      else
        s, e = '{', '}'
        for k, v in pairs(o) do
          table.insert(a, '"' .. tostring(k) .. '":' .. impl(v))
        end
      end
      table.remove(l)
      return s .. table.concat(a, ',') .. e
    elseif type(o) == "string" then
      local s, l = '"', o:len()
      for i = 1, l do
        local c = o:sub(i, i)
        if c == '"' then
          s = s .. '\\"'
        elseif c == '\\' then
          s = s .. '\\\\'
        elseif c == '\b' then
          s = s .. '\\b'
        elseif c == '\f' then
          s = s .. '\\f'
        elseif c == '\n' then
          s = s .. '\\n'
        elseif c == '\r' then
          s = s .. '\\r'
        elseif c == '\t' then
          s = s .. '\\t'
        else
          s = s .. c
        end
      end
      return s .. '"'
    elseif type(o) == "number" then
      return o
    elseif type(o) == "boolean" then
      return o and "true" or "false"
    end
  end
  return impl(o)
end

JSON = { parse = parse, stringify = stringify, null = null }
