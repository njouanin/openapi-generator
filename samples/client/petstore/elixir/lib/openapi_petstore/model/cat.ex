# NOTE: This file is auto generated by OpenAPI Generator 7.0.0-SNAPSHOT (https://openapi-generator.tech).
# Do not edit this file manually.

defmodule OpenapiPetstore.Model.Cat do
  @moduledoc """
  
  """

  @derive Jason.Encoder
  defstruct [
    :class_name,
    :color,
    :declawed
  ]

  @type t :: %__MODULE__{
    :class_name => String.t,
    :color => String.t | nil,
    :declawed => boolean() | nil
  }
end

